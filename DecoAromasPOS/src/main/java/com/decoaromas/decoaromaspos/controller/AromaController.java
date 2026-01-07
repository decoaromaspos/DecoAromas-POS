package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.aroma.AromaCantidadProductosResponse;
import com.decoaromas.decoaromaspos.dto.aroma.AromaRequest;
import com.decoaromas.decoaromaspos.dto.aroma.AromaResponse;
import com.decoaromas.decoaromaspos.dto.other.request.DeletedIdRequest;
import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.dto.other.response.ValidationErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.service.AromaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/aromas")
@RequiredArgsConstructor
@Tag(name = "Gestión de Aromas", description = "API para crear, leer, actualizar y eliminar aromas.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(
                                name = "Ejemplo 401",
                                summary = "Token inválido o faltante",
                                value = "{\"path\": \"/api/aromas\", " +
                                        "\"error\": \"No autorizado\", " +
                                        "\"message\": \"Se requiere autenticación para acceder a este recurso. El token puede ser inválido o haber expirado.\", " +
                                        "\"status\": 401}"
                        )
                )
        )
})
public class AromaController {

    private final AromaService aromaService;

    @Operation(summary = "Listar todos los aromas", description = "Obtiene una lista completa de todos los aromas registrados en la base de datos.")
    @ApiResponse(responseCode = "200", description = "Lista de aromas obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AromaResponse.class)))
    )
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<AromaResponse>> listarAromas() {
        return ResponseEntity.ok(aromaService.listarAromas());
    }

    @Operation(summary = "Listar aromas activos", description = "Devuelve solo los aromas que no han sido marcados como eliminados (soft delete).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de activos obtenida correctamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AromaResponse.class))))
    })
    @GetMapping("/activos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<AromaResponse>> listarActivos() {
        return ResponseEntity.ok(aromaService.listarAromasActivos());
    }

    @Operation(summary = "Listar aromas paginados y filtrados", description = "Devuelve una lista paginada de aromas aplicando filtros dinámicos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginación de aromas obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    })
    @GetMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<AromaCantidadProductosResponse>> listarAromasFiltrados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "nombre") String sortBy,
            @Parameter(description = "Nombre parcial del aroma") @RequestParam(required = false) String nombre,
            @Parameter(description = "Estado de aroma: true (desactivado), false (activo)") @RequestParam(required = false) Boolean isDeleted) {
        return ResponseEntity.ok(aromaService.getAromasFiltrados(page, size, sortBy, nombre, isDeleted));
    }

    @Operation(summary = "Verificar disponibilidad de nombre", description = "Comprueba si un nombre de aroma ya existe para evitar duplicados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El nombre está disponible",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvailabilityResponse.class),
                            examples = @ExampleObject(
                                    summary = "nombre disponible",
                                    value = "{\"message\": \"Nombre disponible.\", \"available\": true}"
                            ))),
            @ApiResponse(responseCode = "409", description = "El nombre ya está en uso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvailabilityResponse.class),
                            examples = @ExampleObject(
                                    summary = "nombre no disponible",
                                    value = "{\"message\": \"El nombre exacto 'almendras' está en uso por algún aroma. Ingrese otro.\", \"available\": false}"
                            )))
    })
    @GetMapping("/check-nombre")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkNombreAvailabilityAroma(@RequestParam String nombre) {
        AvailabilityResponse response = aromaService.checkNombreAvailability(nombre);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Buscar aromas por nombre parcial", description = "Devuelve aromas que coincidan parcialmente con el nombre.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de aromas obtenida segun nombre",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = AromaResponse.class))))
    })
    @GetMapping("/buscar/nombre/{nombre}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<AromaResponse>> buscarAromasPorNombreParcial(@PathVariable String nombre) {
        return ResponseEntity.ok(aromaService.buscarAromaPorNombreParcial(nombre));
    }

    @Operation(summary = "Obtener aroma por ID", description = "Recupera el detalle de un aroma específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aroma encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AromaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aroma no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AvailabilityResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 404",
                                    summary = "nombre no disponible",
                                    value = "{\"error\": \"No existe aroma con id 1\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 404}"
                            )))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AromaResponse> obtenerAromaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(aromaService.obtenerAromaPorId(id));
    }

    @Operation(summary = "Crear aroma", description = "Crea un nuevo aroma. Valida unicidad del nombre y formato.")
    @ApiResponses(value = {
            // 201: Éxito
            @ApiResponse(responseCode = "201", description = "Aroma creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AromaResponse.class))),

            // 400: Error de validación (@Valid falla en AromaRequest)
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. nombre vacío)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),

            // 409: Conflicto (ExistsRegisterException lanzada por el servicio)
            @ApiResponse(responseCode = "409", description = "El nombre del aroma ya existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class))),

            // 403: Seguridad
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario sin rol ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )
                    )
            )
    })
    @PostMapping
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<AromaResponse> crearAroma(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos requeridos para el nuevo aroma")
            @Valid @RequestBody AromaRequest aroma) {
        // Servicio lanza ExistsRegisterException si el nombre existe
        return new ResponseEntity<>(aromaService.crearAroma(aroma), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar aroma", description = "Modifica los datos de un aroma existente.")
    @ApiResponses(value = {
            // 201: Éxito
            @ApiResponse(responseCode = "201", description = "Aroma actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AromaResponse.class))),

            // 409: Conflicto (ExistsRegisterException lanzada por el servicio)
            @ApiResponse(responseCode = "409", description = "El nombre del aroma ya existe, no se puede actualizar el aroma",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class))),

            // 403: Seguridad
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario no ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )
                    )
            )
    })
    @PutMapping("/update/{id}")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<AromaResponse> actualizarAroma(@PathVariable Long id, @Valid @RequestBody AromaRequest aroma) {
        return ResponseEntity.ok(aromaService.actualizarAroma(id, aroma));
    }

    @Operation(summary = "Cambiar estado eliminado", description = "Activa o desactiva (soft delete) un aroma.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aroma cambia de estado correctamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AromaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Aroma no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    summary = "Aroma no encontrado segun id",
                                    value = "{\"error\": \"No existe aroma con id 1654\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 404}"
                            )
                    )
            )
    })
    @PutMapping("/cambiar/estado")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<AromaResponse> cambiarEstadoEliminado(@RequestBody DeletedIdRequest request) {
        return ResponseEntity.ok(aromaService.cambiarEstadoEliminadoAroma(request.getId(), request.getDeleted()));
    }

    @Operation(summary = "Eliminar aroma físicamente", description = "Elimina permanentemente un aroma de la base de datos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Aroma eliminado correctamente (sin contenido)"),
            @ApiResponse(responseCode = "404", description = "Aroma no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    summary = "Aroma no encontrado segun id",
                                    value = "{\"error\": \"No existe aroma con id 1654\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 404}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario no ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )
                    )
            ),
            // 409: Conflicto (DataIntegrityViolationException lanzada por el servicio)
            @ApiResponse(responseCode = "409", description = "No se puede eliminar aroma con productos asociados",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    summary = "Existen productos asociados",
                                    value = "{\"error\": \"No se puede eliminar el aroma. Tiene 1 productos asociados.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 409}"
                            )
                    )),
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarAroma(@PathVariable Long id) {
        aromaService.eliminarAroma(id);
        return ResponseEntity.noContent().build();
    }
}