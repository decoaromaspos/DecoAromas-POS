package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoFilterDTO;
import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoInventarioResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.dto.other.response.ValidationErrorResponse;
import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.MovimientoInventarioService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/movimientos/inventario")
@RequiredArgsConstructor
@Tag(name = "Gestión de Movimientos de Inventario", description = "API para consultar el historial de movimientos de stock (entradas/salidas).")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/movimientos\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class MovimientoInventarioController {

    private final MovimientoInventarioService movService;

    @Operation(summary = "Listar todos los movimientos", description = "Obtiene una lista completa del historial de movimientos de inventario.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MovimientoInventarioResponse.class))))
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<MovimientoInventarioResponse>> listarMovimientos() {
        return ResponseEntity.ok(movService.listarMovimientos());
    }

    @Operation(summary = "Listar movimientos paginados", description = "Devuelve una lista paginada de movimientos de inventario (sin filtros).")
    @ApiResponse(responseCode = "200", description = "Paginación obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<MovimientoInventarioResponse>> listarMovimientosPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "movimientoId") String sortBy) {
        return ResponseEntity.ok(movService.obtenerMovimientosPaginados(page, size, sortBy));
    }

    @Operation(summary = "Listar movimientos paginados y filtrados", description = "Devuelve una lista paginada y filtrada de movimientos de inventario por múltiples criterios.")
    @ApiResponse(responseCode = "200", description = "Paginación obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @PostMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<MovimientoInventarioResponse>> getMovimientosFiltrados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "fecha") String sortBy,

            @Parameter(description = "Fecha de inicio del rango (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha de fin del rango (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Valid @RequestBody MovimientoFilterDTO filters) {

        return ResponseEntity.ok(movService.getMovimientosFiltrados(page, size, sortBy, fechaInicio, fechaFin, filters));
    }

    @Operation(summary = "Obtener movimiento por ID", description = "Devuelve el movimiento correspondiente al ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MovimientoInventarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "ID de movimiento inválido (debe ser numérico)", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movimiento no encontrado", content = @Content(schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(value = "{\"error\": \"Movimiento con id 5001 no encontrado.\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 404}")))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<MovimientoInventarioResponse> obtenerMovimientoPorId(
            @Parameter(description = "ID del movimiento", example = "5001") @PathVariable Long id) {
        return ResponseEntity.ok(movService.obtenerMovimientoPorId(id));
    }

    @Operation(summary = "Obtener movimientos por fecha", description = "Devuelve los movimientos de inventario de una fecha específica.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente según fecha",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = MovimientoInventarioResponse.class))))
    @GetMapping("/dia")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<MovimientoInventarioResponse>> obtenerMovimientosPorFecha (
            @Parameter(description = "Fecha exacta a consultar (yyyy-MM-dd)", required = true, example = "2025-12-10")
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(movService.obtenerMovimientosPorFecha(fecha));
    }

    @Operation(summary = "Obtener movimientos por usuario", description = "Devuelve los movimientos de inventario de un usuario específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente según id de usuario", content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = MovimientoInventarioResponse.class)))),
            @ApiResponse(responseCode = "400", description = "ID de usuario inválido (debe ser numérico)", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(value = "{\"error\": \"Usuario con id 123 no encontrado.\"," + "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," + "\"status\": 404}")))
    })
    @GetMapping("/usuario/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<MovimientoInventarioResponse>> obtenerMovimientosPorIdUsuario(
            @Parameter(description = "ID del usuario cajero/admin", example = "3") @PathVariable Long id){
        return ResponseEntity.ok(movService.obtenerMovimientosPorIdUsuario(id));
    }

    @Operation(summary = "Obtener movimientos por producto", description = "Devuelve la trazabilidad completa (historial) de movimientos de stock de un producto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente según id de producto", content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = MovimientoInventarioResponse.class)))),
            @ApiResponse(responseCode = "400", description = "ID de producto inválido (debe ser numérico)", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(value = "{\"error\": \"Producto con id 123 no encontrado.\"," + "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," + "\"status\": 404}")))
    })
    @GetMapping("/producto/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<MovimientoInventarioResponse>> obtenerMovimientosPorIdProducto(
            @Parameter(description = "ID del producto", example = "10") @PathVariable Long id) {
        return ResponseEntity.ok(movService.obtenerMovimientosPorProductoId(id));
    }

    @Operation(summary = "Obtener movimientos por motivo", description = "Devuelve los movimientos de inventario agrupados por la causa del movimiento (VENTA, AJUSTE_VENTA, PRODUCCION, NUEVO_STOCK, CORRECION).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente según motivo de movimiento.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation =  MovimientoInventarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Motivo de movimiento inválido", content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
    })
    @GetMapping("/motivo/{motivo}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<MovimientoInventarioResponse>> obtenerMovimientosPorMotivo(
            @Parameter(description = "Motivo del movimiento (VENTA, AJUSTE, etc.)", example = "AJUSTE") @PathVariable MotivoMovimiento motivo) {
        return ResponseEntity.ok(movService.obtenerMovimientosPorMotivo(motivo));
    }
}
