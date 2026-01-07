package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.caja.AbrirCajaRequest;
import com.decoaromas.decoaromaspos.dto.caja.CajaResponse;
import com.decoaromas.decoaromaspos.dto.caja.CajaResumenResponse;
import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.dto.other.response.ValidationErrorResponse;
import com.decoaromas.decoaromaspos.enums.EstadoCaja;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.CajaService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/cajas")
@RequiredArgsConstructor
@Tag(name = "Gestión de Cajas", description = "API para el control de flujo de efectivo: apertura, cierre, cuadratura y reportes de cajas.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/cajas\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class CajaController {

    private final CajaService cajaService;

    @Operation(summary = "Listar todas las cajas", description = "Obtiene el historial completo de cajas (abiertas y cerradas).")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CajaResponse.class))))
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<CajaResponse>> listarCajas() {
        return ResponseEntity.ok(cajaService.listarCajas());
    }


    @Operation(summary = "Listar cajas paginadas y filtradas", description = "Devuelve una lista paginada de cajas aplicando filtros dinámicos.")
    @ApiResponse(responseCode = "200", description = "Paginación obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<CajaResponse>> getMovimientosFiltrados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "fechaApertura") String sortBy,
            @Parameter(description = "Fecha inicio (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @Parameter(description = "Fecha fin (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Parameter(description = "Estado de la caja (ABIERTA, CERRADA)") @RequestParam(required = false) EstadoCaja estado,
            @Parameter(description = "Filtrar por cuadratura: true (cuadradas), false (con diferencia), null (todas)") @RequestParam(required = false) Boolean cuadrada,
            @Parameter(description = "ID del usuario cajero") @RequestParam(required = false) Long usuarioId) {

        return ResponseEntity.ok(cajaService.getCajasFiltradas(page, size, sortBy, fechaInicio, fechaFin, estado, cuadrada, usuarioId));
    }


    @Operation(summary = "Obtener caja por ID", description = "Devuelve la caja correspondiente al ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CajaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Caja no encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(value = "{\"error\": \"No existe caja con id 1\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 404}")))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<CajaResponse> obtenerCajaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cajaService.obtenerCajaPorId(id));
    }

    @Operation(summary = "Obtener caja abierta", description = "Devuelve la caja que está actualmente abierta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caja abierta encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CajaResponse.class))),
            @ApiResponse(responseCode = "404", description = "No hay ninguna caja abierta actualmente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No hay ninguna caja abierta.\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 404}")))
    })
    @GetMapping("/abierta")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<CajaResponse> obtenerCajaAbierta() {
        return ResponseEntity.ok(cajaService.obtenerCajaAbierta());
    }

    @Operation(summary = "Obtener resumen de caja por ID", description = "Calcula los totales de ventas agrupados por medio de pago para una caja específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Obtener resumen correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CajaResumenResponse.class))),
            @ApiResponse(responseCode = "404", description = "Caja con Id no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"Caja con id 123 no encontrada.\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 404}")))
    })
    @GetMapping("/{id}/resumen")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<CajaResumenResponse> obtenerResumenCajaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cajaService.obtenerResumenCajaById(id));
    }

    @Operation(summary = "Obtener resumen de caja abierta", description = "Calcula los totales en tiempo real de la caja actual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Obtener resumen correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CajaResumenResponse.class))),
            @ApiResponse(responseCode = "404", description = "No hay ninguna caja abierta actualmente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No hay caja abierta.\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 404}")))
    })
    @GetMapping("/abierta/resumen")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<CajaResumenResponse> obtenerResumenCajaAbierta() {
        return ResponseEntity.ok(cajaService.obtenerResumenCajaAbierta());
    }

    @Operation(summary = "Abrir caja", description = "Inicia un nuevo turno de caja. Requiere que no exista otra caja abierta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Caja abierta exitosamente",
                    content = @Content(mediaType = "application/json",schema = @Schema(implementation = CajaResponse.class))),

            @ApiResponse(responseCode = "400", description = "Datos inválidos (monto negativo, usuario nulo)",
                    content = @Content(mediaType = "application/json",schema = @Schema(implementation = ValidationErrorResponse.class))),

            @ApiResponse(responseCode = "500", description = "Error de negocio: Ya existe una caja abierta",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(summary = "Caja ya abierta", value = "{\"error\": \"Ya existe una caja abierta. Debe cerrarla antes de abrir una nueva.\", \"status\": 500}")))
    })
    @PostMapping("/abrir")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN)
    public ResponseEntity<CajaResponse> abrirCaja(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos de apertura")
            @Valid @RequestBody AbrirCajaRequest request) {
        return new ResponseEntity<>(cajaService.abrirCaja(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Cerrar caja abierta", description = "Cierra el turno actual, calcula diferencias y guarda los totales. Se recibe el efectivo físico contado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caja cerrada correctamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CajaResponse.class))),

            @ApiResponse(responseCode = "404", description = "No hay caja abierta para cerrar",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No hay ninguna caja abierta.\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 404}"))),

            @ApiResponse(responseCode = "500", description = "Error de negocio (Efectivo nulo)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Debe ingresar efectivo de cierre.\", \"timestamp\": \"2025-11-29T17:06:17.666708502\", \"status\": 500}"))),
    })
    @PostMapping("/cerrar-abierta/efectivo/{efectivoReal}")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN)
    public ResponseEntity<CajaResponse> cerrarCaja(
            @Parameter(description = "Monto de efectivo contado físicamente en la caja", required = true, example = "150500.0")
            @Valid @PathVariable Double efectivoReal) {
        return ResponseEntity.ok(cajaService.cerrarCaja(efectivoReal));
    }

    @Operation(summary = "Eliminar caja", description = "Borrado físico de una caja. Solo Super Admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Caja eliminada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CajaResponse.class))),

            @ApiResponse(responseCode = "403", description = "Requiere rol SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"))),

            @ApiResponse(responseCode = "409", description = "Error de integridad: No se puede borrar si tiene ventas asociadas",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class)))
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarCaja(@PathVariable Long id) {
        cajaService.eliminarCaja(id);
        return ResponseEntity.noContent().build();
    }
}
