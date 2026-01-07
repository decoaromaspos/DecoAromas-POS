package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.cotizacion.CotizacionRequest;
import com.decoaromas.decoaromaspos.dto.cotizacion.CotizacionResponse;
import com.decoaromas.decoaromaspos.dto.cotizacion.CotizacionUpdateEstado;
import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.CotizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.IS_AUTHENTICATED;
import static com.decoaromas.decoaromaspos.utils.SecurityConstants.IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN;

@RestController
@RequestMapping("/api/cotizaciones")
@RequiredArgsConstructor
@Tag(name = "Gestión de Cotizaciones", description = "API para crear, leer, actualizar y eliminar cotizaciones de venta.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(
                                name = "Ejemplo 401",
                                summary = "Token inválido o faltante",
                                value = "{\"path\": \"/api/cotizaciones\", \"error\": \"No autorizado\", \"message\": \"Se requiere autenticación.\", \"status\": 401}"
                        )
                )
        )
})
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @Operation(
            summary = "Obtener cotizaciones paginadas y filtradas",
            description = "Devuelve una lista paginada de cotizaciones aplicando filtros dinámicos. Ideal para tablas de consulta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginación de cotizaciones obtenida correctamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    })
    @GetMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<CotizacionResponse>> getCotizacionesFiltradas(
            @Parameter(description = "Número de página (base 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo para ordenar (ej: 'fechaEmision')", example = "fechaEmision") @RequestParam(defaultValue = "fechaEmision") String sortBy,
            @Parameter(description = "Fecha de inicio del rango (formato YYYY-MM-DD)", example = "2024-10-20") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin del rango (formato YYYY-MM-DD)", example = "2024-10-25") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Filtrar por tipo de cliente (DETALLE o MAYORISTA)", example = "DETALLE") @RequestParam(required = false) TipoCliente tipoCliente,
            @Parameter(description = "Monto total neto mínimo", example = "10000") @RequestParam(required = false) Double minTotalNeto,
            @Parameter(description = "Monto total neto máximo", example = "50000") @RequestParam(required = false) Double maxTotalNeto,
            @Parameter(description = "Filtrar por ID de usuario creador", example = "1") @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Filtrar por ID de cliente", example = "5") @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(
                cotizacionService.getCotizacionesFiltradas(
                        page, size, sortBy, fechaInicio, fechaFin,
                        tipoCliente, minTotalNeto, maxTotalNeto, usuarioId, clienteId));
    }

    @Operation(
            summary = "Listar todas las cotizaciones",
            description = "Obtiene una lista completa de todas las cotizaciones sin paginación. Usar con precaución."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de cotizaciones",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CotizacionResponse.class)))
    })
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<CotizacionResponse>> getCotizaciones() {
        return ResponseEntity.ok(cotizacionService.listarCotizaciones());
    }

    @Operation(
            summary = "Crear una nueva cotización",
            description = "Crea una nueva cotización con sus detalles y la guarda en la base de datos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cotización creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CotizacionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Usuario no encontrado según id",
                            value = "{\"error\": \"No existe usuario con id 232\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "500", description = "Restricción lógica de negocio", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusinessException.class),
                    examples = @ExampleObject(
                            name = "Ejemplo 500",
                            summary = "Restricción lógica negocio, descuento negativo, descuento entre 0 y 100, descuento menor al precio de producto.",
                            value = "{\"error\": \"El porcentaje de descuento debe estar entre 0 y 100.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 500}"
                    ))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos (Validación fallida)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IllegalArgumentException.class)))
    })
    @PostMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<CotizacionResponse> crearCotizacion(@Valid @RequestBody CotizacionRequest request) {
        CotizacionResponse response = cotizacionService.crearCotizacion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Actualizar el estado de una cotización",
            description = "Permite cambiar el estado de una cotización (ej: PENDIENTE, APROBADA, RECHAZADA, CONVERTIDA)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CotizacionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada",  content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Cotización no encontrado según id",
                            value = "{\"error\": \"No existe cotización con id 232\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "403", description = "Requiere rol VENDEDOR, ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario sin rol VENDEDOR, ADMIN o SUPER_ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )))
    })
    @PatchMapping("/cambiar-estado")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<CotizacionResponse> cambiarEstadoCotizacion(@Valid @RequestBody CotizacionUpdateEstado request) {
        return ResponseEntity.ok(cotizacionService.actualizarEstadoCotizacion(request));
    }

    @Operation(
            summary = "Eliminar una cotización",
            description = "Elimina permanentemente una cotización de la base de datos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cotización eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cotización no encontrada",  content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Cotización no encontrado según id",
                            value = "{\"error\": \"No existe cotización con id 232\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "403", description = "Requiere rol VENDEDOR, ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario sin rol VENDEDOR, ADMIN o SUPER_ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )))
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarCotizacion(
            @Parameter(description = "ID de la cotización a eliminar", required = true, example = "123")
            @PathVariable Long id) {
        cotizacionService.eliminarCotizacion(id);
        return ResponseEntity.noContent().build();
    }
}