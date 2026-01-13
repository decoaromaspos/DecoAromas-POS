package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.request.RestoreRequest;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.IS_ADMIN_OR_SUPER_ADMIN;
import static com.decoaromas.decoaromaspos.utils.SecurityConstants.IS_SUPER_ADMIN;

@RestController
@RequestMapping("/api/backups")
@AllArgsConstructor
@Tag(name = "Gestión de Backups", description = "API para generar respaldos de base de datos y restaurarlos (pg_dump / pg_restore).")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(
                                value = """
                                        {
                                            "path": "/api/backups",
                                            "error": "No autorizado",
                                            "message": "Se requiere autenticación para acceder a este recurso. El token puede ser inválido o haber expirado.",
                                            "status": 401
                                        }
                                        """
                        )
                )
        )
})
public class BackupController {

    private final BackupService backupService;


    @Operation(summary = "Crear backup de base de datos manual",
            description = "Ejecuta 'pg_dump' en el contenedor de base de datos para generar un archivo .dump en la ruta C:\\Backups\\decoaromas\\ con la fecha y hora actual (Windows).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Backup creado exitosamente",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Backup creado con éxito: decoaromas_2025-11-29_10-00.dump"))),
            @ApiResponse(responseCode = "500", description = "Error interno al ejecutar pg_dump",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Error al crear el backup: Fallo en pg_dump. Código de salida: 1..."))),
            @ApiResponse(responseCode = "503", description = "Operación de backup interrumpida",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "La operación de backup fue interrumpida.")))
    })
    @PostMapping("/create")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<String> createBackup() {
        try {
            String result = backupService.createBackup();
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            // Error normal de entrada/salida
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al crear el backup: " + e.getMessage());
        } catch (InterruptedException e) {
            // Restablecer el flag de interrupción
            Thread.currentThread().interrupt();
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("La operación de backup fue interrumpida.");
        }
    }

    @Operation(summary = "Listar backups disponibles", description = "Devuelve una lista de nombres de archivos .dump almacenados en la ruta C:\\Backups\\decoaromas\\ (Windows).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(type = "string", example = "decoaromas_2025-11-29_10-00.dump")))),
            @ApiResponse(responseCode = "500", description = "Error al leer el directorio de backups")
    })
    @GetMapping("/list")
    public ResponseEntity<List<String>> listBackups() {
        try {
            List<String> files = backupService.listBackups();
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @Operation(summary = "Restaurar base de datos", description = "PELIGRO: Ejecuta 'pg_restore'. Borra la base de datos actual y carga el backup seleccionado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Restauración completada",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Restauración completada con éxito desde el archivo: decoaromas_..."))),

            @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido o no proporcionado",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Error de validación: El nombre del archivo de backup es requerido."))),

            @ApiResponse(responseCode = "403", description = "Requiere rol SUPER_ADMIN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class))),

            @ApiResponse(responseCode = "500", description = "Error crítico durante la restauración (pg_restore falló)",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Error al restaurar el backup: Fallo en pg_restore. Código de salida: 1..."))),
            @ApiResponse(responseCode = "503", description = "Operación de restaurar backup interrumpida",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "La restauración fue interrumpida debido a una señal del sistema.")))
    })
    @PostMapping("/restore")
    @PreAuthorize(IS_SUPER_ADMIN)
    public ResponseEntity<String> restoreBackup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Objeto con el nombre del archivo a restaurar", required = true)
            @RequestBody RestoreRequest request) {
        if (request == null || request.getFilename() == null || request.getFilename().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre del archivo de backup es requerido.");
        }

        try {
            String result = backupService.restoreBackup(request.getFilename());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body("Error de validación: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al restaurar el backup: " + e.getMessage());
        } catch (InterruptedException e) {
            // Restauramos el flag de interrupción para cumplir con java:S2142
            Thread.currentThread().interrupt();
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("La restauración fue interrumpida debido a una señal del sistema.");
        }
    }


    @Operation(summary = "Crear backup para Nueva Sucursal (Smart Dump)",
            description =
                    """
                        Genera un archivo .dump manipulado mediante una transacción aislada (Snapshot).\s
                        En el archivo resultante: 1) Todos los productos tendrán Stock = 0.\s
                        2) Se eliminan todos los usuarios excepto el que tenga rol SUPER_ADMIN.\s
                        "NOTA: La base de datos original NO sufre modificaciones, es un proceso seguro."
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Backup de sucursal creado exitosamente",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Backup de inventario creado correctamente: decoaromas_inventario_2025-01-07_10-00.dump"))),
            @ApiResponse(responseCode = "500", description = "Error interno (SQL o pg_dump)",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Error crítico al generar backup de sucursal: Error en pg_dump..."))),
            @ApiResponse(responseCode = "503", description = "Operación interrumpida",
                    content = @Content(mediaType = "text/plain"))
    })
    @PostMapping("/create-smart-inventario")
    @PreAuthorize(IS_SUPER_ADMIN)
    public ResponseEntity<String> createSmartDemoBackup() {
        try {
            String result = backupService.createSmartDemoBackup1();
            return ResponseEntity.ok(result);
        } catch (InterruptedException e) {
            // Manejo específico para interrupción de hilos
            Thread.currentThread().interrupt();
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("La operación de backup fue interrumpida.");
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error crítico al generar backup de inventario: " + e.getMessage());
        }
    }
}