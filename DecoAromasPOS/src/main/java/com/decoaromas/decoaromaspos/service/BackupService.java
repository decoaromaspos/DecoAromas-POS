package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final JdbcTemplate jdbcTemplate;

    // Ruta de backups dentro del contenedor (montada a C:\Backups\decoaromas)
    private static final String BACKUP_DIR_PATH = "/app/backups";
    private static final String FILENAME_PREFIX = "decoaromas_";
    private static final String FILENAME_SUFFIX = ".dump";

    // Inyectamos las variables de entorno de la DB
    @Value("${DB_HOST}")
    private String dbHost;
    @Value("${DB_PORT}")
    private String dbPort;
    @Value("${DB_USER}")
    private String dbUser;
    @Value("${DB_NAME}")
    private String dbName;
    @Value("${DB_PASSWORD}")
    private String dbPassword;


    // Lista blanca de tablas que SI llevarán datos
    private static final List<String> TABLES_WITH_DATA = Arrays.asList(
            "producto", "aroma", "familia_producto", "usuario"
    );
    private static final String TEMP_DB_PREFIX = "decoaromas_staging_";


    /**
     * Crea un backup de la estructura completa, pero con productos, aromas y familias.
     * Considera a todos los productos con stock 0. Y mantiene solo al usuario súper administrador.
     */
    public String createSmartDemoBackup1() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
        String timestamp = DateUtils.obtenerFechaHoraActual().format(formatter);

        // Nombre del archivo final
        String finalFilename = "decoaromas_inventario_" + timestamp + ".dump";

        // Nombre de la Base de Datos Temporal
        String tempDbName = TEMP_DB_PREFIX + System.currentTimeMillis();

        Path backupPath = ensureBackupDirectoryExists();
        String tempDumpPath = backupPath.resolve("temp_transfer_" + timestamp + ".dump").toString();
        String finalDumpPath = backupPath.resolve(finalFilename).toString();

        try {
            // PASO 1: CREAR BASE DE DATOS TEMPORAL
            // Usamos una conexión directa a la BD 'postgres' (u otra default) para crear la nueva BD
            try (Connection conn = DriverManager.getConnection("jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres", dbUser, dbPassword);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE " + tempDbName);
            }

            // PASO 2: CLONAR DATOS (Filtrando tablas pesadas)
            // Primero: Hacemos dump de la original (con filtros) a un archivo temporal
            // Esto copia toda la estructura pero datos solo de la lista blanca.
            List<String> dumpCmd = new ArrayList<>();
            dumpCmd.add("pg_dump");
            dumpCmd.add("-h"); dumpCmd.add(dbHost);
            dumpCmd.add("-p"); dumpCmd.add(dbPort);
            dumpCmd.add("-U"); dumpCmd.add(dbUser);
            dumpCmd.add("-F"); dumpCmd.add("c"); // Custom format
            dumpCmd.add("-d"); dumpCmd.add(dbName); // Origen: Producción
            dumpCmd.add("-f"); dumpCmd.add(tempDumpPath);

            // Excluir datos de tablas que no están en la lista blanca
            List<String> allTables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                    String.class
            );

            for (String table : allTables) {
                if (!TABLES_WITH_DATA.contains(table)) {
                    dumpCmd.add("--exclude-table-data=" + table);
                }
            }

            runProcess(dumpCmd);

            // Segundo: Restauramos ese archivo en la BD Temporal
            List<String> restoreCmd = Arrays.asList(
                    "pg_restore", "-h", dbHost, "-p", dbPort, "-U", dbUser,
                    "-d", tempDbName, // Destino: Temporal
                    tempDumpPath
            );
            runProcess(restoreCmd);

            // PASO 3: MODIFICAR LA BD TEMPORAL (Aquí hacemos COMMIT real)
            // Nos conectamos a la BD TEMPORAL para hacer las limpiezas
            String tempDbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + tempDbName;

            try (Connection connTemp = DriverManager.getConnection(tempDbUrl, dbUser, dbPassword);
                 Statement stmtTemp = connTemp.createStatement()) {

                // A. Activamos Modo Dios (Replica) para poder borrar sin líos de FK
                stmtTemp.execute("SET session_replication_role = 'replica'");

                // B. Borramos usuarios (COMMIT automático porque no desactivamos autoCommit)
                stmtTemp.executeUpdate("DELETE FROM usuario WHERE rol <> 'SUPER_ADMIN'");

                // C. Reseteamos reglas
                stmtTemp.execute("SET session_replication_role = 'origin'");

                // D. Stock a 0 (Ya con reglas activas, es seguro)
                stmtTemp.executeUpdate("UPDATE producto SET stock = 0");
            }

            // PASO 4: GENERAR EL BACKUP FINAL DESDE LA BD TEMPORAL
            List<String> finalDumpCmd = Arrays.asList(
                    "pg_dump", "-h", dbHost, "-p", dbPort, "-U", dbUser,
                    "-F", "c", "-v", "-b",
                    "-d", tempDbName, // Origen: La BD Temporal ya modificada
                    "-f", finalDumpPath
            );
            runProcess(finalDumpCmd);

            return "Backup de inventario creado correctamente: " + finalFilename;

        } finally {
            // LIMPIEZA: BORRAR BD TEMPORAL Y ARCHIVO INTERMEDIO
            try {
                Files.deleteIfExists(Path.of(tempDumpPath));  // Borrar archivo intermedio

                // Borrar BD Temporal (Conectando a 'postgres' de nuevo)
                try (Connection conn = DriverManager.getConnection("jdbc:postgresql://" + dbHost + ":" + dbPort + "/postgres", dbUser, dbPassword);
                     Statement stmt = conn.createStatement()) {
                    // Forzamos desconexión de posibles clientes a la BD temporal antes de borrarla
                    stmt.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + tempDbName + "'");
                    stmt.execute("DROP DATABASE IF EXISTS " + tempDbName);
                }
            } catch (Exception e) {
                // Solo loguear, no interrumpir el flujo si ya se creó el backup
                System.err.println("Advertencia limpiando temporales: " + e.getMessage());
            }
        }
    }

    // Helper para ejecutar comandos de sistema
    private void runProcess(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", dbPassword);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes());
            throw new IOException("Error ejecutando comando: " + command.get(0) + ". Detalle: " + error);
        }
    }



    /**
     * Ejecuta el comando pg_restore para cargar el archivo .dump en la base de datos.
     * Se requiere el nombre del archivo de backup.
     */
    public String restoreBackup(String filename) throws IOException, InterruptedException {
        // 1. Validación de seguridad y formato del nombre de archivo
        if (filename == null || filename.contains("..") || !filename.endsWith(FILENAME_SUFFIX)) {
            throw new IllegalArgumentException("Nombre de archivo de backup inválido.");
        }

        Path backupPath = ensureBackupDirectoryExists(); // Asegura que la ruta de backups exista
        Path fullPath = backupPath.resolve(filename);

        // 2. Comprobar que el archivo exista en el sistema
        if (!Files.exists(fullPath)) {
            throw new IOException("El archivo de backup no existe: " + fullPath);
        }

        // 3. Comando pg_restore
        String[] command = {
                "pg_restore",
                "-h", dbHost,
                "-p", dbPort,
                "-U", dbUser,
                "-c", // Elimina los objetos de la DB antes de restaurarlos.
                "-d", dbName,
                fullPath.toString() // Ruta completa del archivo .dump
        };

        // 4. Ejecutar el proceso con la contraseña
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", dbPassword); // Pasar la contraseña de forma segura

        Process process = pb.start();

        // La restauración de un archivo grande puede tardar; aumentamos el tiempo de espera si fuera necesario.
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes());
            throw new IOException("Fallo en pg_restore. Código de salida: " + exitCode + ". Error: " + error);
        }

        return "Restauración completada con éxito desde el archivo: " + filename;
    }

    /**
     * Crea un directorio de backups si no existe.
     */
    private Path ensureBackupDirectoryExists() throws IOException {
        Path backupPath = Paths.get(BACKUP_DIR_PATH);
        if (!Files.exists(backupPath)) {
            // Crea el directorio, necesario si está vacío
            Files.createDirectories(backupPath);
        }
        return backupPath;
    }

    /**
     * 1. Genera un nombre de archivo con formato de fecha.
     * 2. Ejecuta el comando pg_dump para crear el archivo .dump (formato custom)
     */
    public String createBackup() throws IOException, InterruptedException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
        String timestamp = DateUtils.obtenerFechaHoraActual().format(formatter);

        String filename = FILENAME_PREFIX + timestamp + FILENAME_SUFFIX;

        Path backupPath = ensureBackupDirectoryExists();
        String fullPath = backupPath.resolve(filename).toString();

        // Comando pg_dump para crear el backup en formato 'custom' (-Fc)
        // El comando se ejecuta desde el backend, pero apunta al servicio postgres
        String[] command = {
                "pg_dump",
                "-h", dbHost,
                "-p", dbPort,
                "-U", dbUser,
                "-F", "c", // Formato Custom (binario, eficiente)
                "-b", // Incluir OIDs
                "-v", // Verbose
                "-d", dbName,
                "-f", fullPath // Ruta completa de salida
        };

        // Crear el ProcessBuilder para ejecutar el comando
        ProcessBuilder pb = new ProcessBuilder(command);

        // Es esencial pasar la contraseña como variable de entorno
        pb.environment().put("PGPASSWORD", dbPassword);

        Process process = pb.start(); // Ejecutar el proceso
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String error = new String(process.getErrorStream().readAllBytes());
            throw new IOException("Fallo en pg_dump. Código de salida: " + exitCode + ". Error: " + error);
        }

        return "Backup creado con éxito: " + filename;
    }

    /**
     * Escanea el directorio y devuelve una lista de archivos .dump.
     */
    public List<String> listBackups() throws IOException {
        Path backupPath = ensureBackupDirectoryExists();

        try (Stream<Path> files = Files.list(backupPath)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(FILENAME_SUFFIX))
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
    }
}