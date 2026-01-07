package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.utils.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class BackupService {

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