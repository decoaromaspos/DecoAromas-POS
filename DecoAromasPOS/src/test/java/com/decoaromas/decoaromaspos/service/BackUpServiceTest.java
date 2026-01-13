package com.decoaromas.decoaromaspos.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BackUpServiceTest {

    @InjectMocks
    private BackupService backupService;

    // Mocks necesarios porque el nuevo BackupService usa base de datos
    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    // Ruta temporal segura para Windows
    private Path tempBackupDir;

    @BeforeEach
    void setup() throws Exception {
        // 1. Configurar ruta temporal para Windows (Ej: C:\Users\User\AppData\Local\Temp\decoaromas_test)
        String tempDir = System.getProperty("java.io.tmpdir");
        tempBackupDir = Paths.get(tempDir, "decoaromas_test");

        // Crear el directorio si no existe
        if (!Files.exists(tempBackupDir)) {
            Files.createDirectories(tempBackupDir);
        }

        // 2. Inyectar valores de configuración (Strings)
        setField("dbHost", "localhost");
        setField("dbPort", "5432");
        setField("dbUser", "postgres");
        setField("dbName", "fake_db");
        setField("dbPassword", "fake_pass");

        Path hardcodedWindowsPath = Paths.get("C:", "Backups", "decoaromas");
        if (!Files.exists(hardcodedWindowsPath)) {
            Files.createDirectories(hardcodedWindowsPath);
        }

    }

    @AfterEach
    void cleanup() throws IOException {
        // Limpieza recursiva del directorio temporal creado en Windows
        if (Files.exists(tempBackupDir)) {
            try (Stream<Path> walk = Files.walk(tempBackupDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Ignorar errores de borrado en test cleanup
                            }
                        });
            }
        }
    }

    // Método auxiliar para inyectar campos privados
    private void setField(String fieldName, Object value) throws Exception {
        Field field = BackupService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(backupService, value);
    }

    @Test
    @DisplayName("Test para restaurar el backup, nombre nulo lanza excepcion")
    void restoreBackup_nombreNulo() {
        assertThatThrownBy(() -> backupService.restoreBackup(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Test para restaurar el backup, path traversal lanza IllegalArgumentException")
    void restoreBackup_pathTraversal() {
        assertThatThrownBy(() -> backupService.restoreBackup("../hack.dump"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Test para restaurar el backup, extension invalida lanza IllegalArgumentException")
    void restoreBackup_extensionInvalida() {
        assertThatThrownBy(() -> backupService.restoreBackup("backup.sql"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Test para listar backups, nunca retorna null")
    void listBackups_nuncaNull() throws Exception {
        List<String> backups = backupService.listBackups();
        assertThat(backups).isNotNull();
    }

    @Test
    @DisplayName("Test para listar backups, solo archivos .dump")
    void listBackups_soloDump() throws Exception {
        List<String> backups = backupService.listBackups();

        backups.forEach(name ->
                assertThat(name).endsWith(".dump")
        );
    }

    @Test
    @DisplayName("Test para restaurar el backup, sin variables de entorno lanza Exception")
    void createBackup_sinEnvVars() {
        assertThatThrownBy(() -> backupService.createBackup())
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Test para restaurar el backup, con nombre de archivo invalido lanza excepcion")
    void restoreBackup_filenameNull_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> backupService.restoreBackup(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nombre de archivo de backup inválido");
    }

    @Test
    @DisplayName("Test para restaurar el backup, con nombre de archivo invalido y un path transversal, lanza excepcion")
    void restoreBackup_filenameConPathTraversal_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> backupService.restoreBackup("../hack.dump"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nombre de archivo de backup inválido");
    }

    @Test
    @DisplayName("Test para restaurar el backup, sin extension de archivo valida lanza excepcion")
    void restoreBackup_extensionInvalida_lanzaIllegalArgumentException() {
        assertThatThrownBy(() -> backupService.restoreBackup("backup.sql"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Test para restaurar el backup, pg restaurador falla, lanza excepcion")
    void restoreBackup_pgRestoreFalla_lanzaIOException() throws Exception {
        Path dump = Path.of("/app/backups/test.dump");
        Files.createFile(dump);

        assertThatThrownBy(() -> backupService.restoreBackup("test.dump"))
                .isInstanceOf(IOException.class);
    }
}