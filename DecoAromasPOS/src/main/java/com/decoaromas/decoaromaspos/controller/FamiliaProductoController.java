package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.familia.FamiliaCantidadProductosResponse;
import com.decoaromas.decoaromaspos.dto.familia.FamiliaRequest;
import com.decoaromas.decoaromaspos.dto.familia.FamiliaResponse;
import com.decoaromas.decoaromaspos.dto.other.request.DeletedIdRequest;
import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.dto.other.response.ValidationErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.model.FamiliaProducto;
import com.decoaromas.decoaromaspos.service.FamiliaProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/familias")
@RequiredArgsConstructor
@Tag(name = "Gestión de Familias de Productos", description = "API para crear, leer, actualizar y eliminar familias de productos.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(
                                name = "Ejemplo 401",
                                summary = "Token inválido o faltante",
                                value = "{\"path\": \"/api/familias\", " +
                                        "\"error\": \"No autorizado\", " +
                                        "\"message\": \"Se requiere autenticación para acceder a este recurso. El token puede ser inválido o haber expirado.\", " +
                                        "\"status\": 401}"
                        )
                )
        )
})
public class FamiliaProductoController {

    private final FamiliaProductoService familiaService;

    @Operation(summary = "Listar todas las familias", description = "Obtiene una lista completa de familias de productos.")
    @ApiResponse(responseCode = "200", description = "Lista de familias obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = FamiliaResponse.class)))
    )
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<FamiliaResponse>> listarFamilias() {
        return ResponseEntity.ok(familiaService.listarFamiliasProductos());
    }

    @Operation(summary = "Listar familias activas", description = "Devuelve solo las familias que no están eliminadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de familias activas obtenida correctamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = FamiliaResponse.class))))
    })
    @GetMapping("/activas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<FamiliaResponse>> listarFamiliasActivas() {
        return ResponseEntity.ok(familiaService.listarFamiliasActivos());
    }

    @Operation(summary = "Listar familias paginadas y filtradas", description = "Devuelve una lista paginada de familias aplicando filtros dinámicos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginación de familias obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    })
    @GetMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<FamiliaCantidadProductosResponse>> listarFamiliasFiltrados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "nombre") String sortBy,
            @Parameter(description = "Nombre parcial de familia") @RequestParam(required = false) String nombre,
            @Parameter(description = "Estado de familia: true (desactivada), false (activa)") @RequestParam(required = false) Boolean isDeleted) {
        return ResponseEntity.ok(familiaService.getFamiliasFiltradas(page, size, sortBy, nombre, isDeleted));
    }

    @Operation(summary = "Verificar disponibilidad de nombre", description = "Comprueba si un nombre de familia está disponible.")
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
                                    value = "{\"message\": \"El nombre exacto 'Cítricos' está en uso por alguna familia. Ingrese otro.\", \"available\": false}"
                            )))
    })
    @GetMapping("/check-nombre")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkNombreAvailabilityFamilia(String nombre) {
        AvailabilityResponse response = familiaService.checkNombreAvailability(nombre);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "Obtener familia por ID", description = "Devuelve la familia correspondiente al ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Familia encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliaProducto.class))),
            @ApiResponse(responseCode = "404", description = "Familia no encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    summary = "Familia no encontrada",
                                    value = "{\"error\": \"No existe familia con id 5\", \"timestamp\": \"2025-11-29T17:06:17\", \"status\": 404}"
                            )))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<FamiliaProducto> obtenerFamilia(@PathVariable Long id) {
        return ResponseEntity.ok(familiaService.obtenerFamiliaRealPorId(id));
    }

    @Operation(summary = "Crear familia", description = "Crea una nueva familia de productos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Familia creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliaResponse.class))),

            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),

            @ApiResponse(responseCode = "409", description = "El nombre de la familia ya existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class))),

            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    summary = "Usuario sin permisos",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )
                    )
            )
    })
    @PostMapping
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<FamiliaResponse> crearFamilia(@RequestBody FamiliaRequest familiaProducto) {
        return new ResponseEntity<>(familiaService.crearFamilia(familiaProducto), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar familia", description = "Actualiza los datos de una familia existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Familia actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliaResponse.class))),

            @ApiResponse(responseCode = "409", description = "Conflicto de nombre duplicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class))),

            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class)))
    })
    @PutMapping("/update/{id}")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<FamiliaResponse> actualizarFamilia(@PathVariable Long id, @RequestBody FamiliaRequest familiaProducto) {
        return ResponseEntity.ok(familiaService.actualizarFamiliaProducto(id, familiaProducto));
    }

    @Operation(summary = "Cambiar estado eliminado", description = "Cambia el estado eliminado (true/false) de una familia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado correctamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = FamiliaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Familia no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class)))
    })
    @PutMapping("/cambiar/estado")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<FamiliaResponse> cambiarEstadoEliminado(@RequestBody DeletedIdRequest request) {
        return ResponseEntity.ok(familiaService.cambiarEstadoEliminadoFamiliaProducto(request.getId(), request.getDeleted()));
    }

    @Operation(summary = "Eliminar familia", description = "Elimina una familia por ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Familia eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Familia no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class))),

            // Nota el cambio aquí: Solo SUPER_ADMIN
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    summary = "Usuario ADMIN intentando borrar",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar familia con productos asociados",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    summary = "Existen productos asociados",
                                    value = "{\"error\": \"No se puede eliminar la familia. Tiene 3 productos asociados.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 409}"
                            )
                    )),
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarFamilia(@PathVariable Long id) {
        familiaService.eliminarFamiliaProducto(id);
        return ResponseEntity.noContent().build();
    }

}
