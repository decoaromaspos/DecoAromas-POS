package com.decoaromas.decoaromaspos.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BackUpServiceTest {

    @InjectMocks
    private BackupService backupService;

    @BeforeEach
    void setup() throws Exception {
        backupService = new BackupService();

        // Inyectar valores manualmente (inventados)
        setField("dbHost", "localhost");
        setField("dbPort", "5432");
        setField("dbUser", "postgres");
        setField("dbName", "fake_db");
        setField("dbPassword", "fake_pass");

        Files.createDirectories(Path.of("/app/backups"));
    }

    @AfterEach
    void cleanup() throws Exception {
        Path dir = Path.of("/app/backups");
        if (Files.exists(dir)) {
            try (var stream = Files.list(dir)) {
                stream.forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                });
            }
        }
    }

    private void setField(String field, String value) throws Exception {
        Field f = BackupService.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(backupService, value);
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