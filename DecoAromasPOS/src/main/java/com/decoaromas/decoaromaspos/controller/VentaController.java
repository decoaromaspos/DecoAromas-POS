package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.venta.*;
import com.decoaromas.decoaromaspos.exception.*;
import com.decoaromas.decoaromaspos.service.VentaService;
import com.decoaromas.decoaromaspos.service.exports.VentaExportService;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Gestión de Ventas", description = "API para crear, leer, actualizar y eliminar ventas.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/ventas\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class VentaController {

    private final VentaService ventaService;
    private final VentaExportService ventaExportService;

    @Operation(summary = "Crear venta", description = "Crea una nueva venta, junto con el movimiento de inventario. Validaciones de stock, pagos y caja abierta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venta creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto: Caja no abierta", content = @Content(schema = @Schema(implementation = CajaCerradaException.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, Cliente o Producto no encontrado según id",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"No existe usuario con id 1\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            ))),
            @ApiResponse(responseCode = "500", description = "Stock insuficiente. Descuento inválido (negativo o superior al 100%). Pagos insuficientes",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusinessException.class),
                            examples = {@ExampleObject(
                                            value = "{\"error\": \"Stock insuficiente para producto Mikado\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 500}"
                                    ),
                                    @ExampleObject(
                                            value = "{\"error\": \"El descuento no puede ser mayor al precio del producto.\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 500}"
                                    ),
                                    @ExampleObject(
                                            value = "{\"error\": \"El descuento numérico no puede ser negativo\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 500}"
                                    ),
                                    @ExampleObject(
                                            value = "{\"error\": \"Pago insuficiente. Mondo pagado (3000) es menor al total (4000)\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 500}"
                                    )}
                    ))
    })
    @PostMapping
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN)
    public ResponseEntity<VentaResponse> crearVenta(@RequestBody @Valid VentaRequest request) {
        VentaResponse response = ventaService.crearVenta(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Verificar disponibilidad de número de documento", description = "Comprueba si un número de documento está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Número de documento disponible", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"Correo disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "Número de documento ya existe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"Correo ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-num-doc")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkNumDocAvailability(@RequestParam String numDoc) {
        AvailabilityResponse response = ventaService.checkNumDocumentoAvailability(numDoc);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Actualizar documento de venta", description = "Asigna o cambia el número de documento de una venta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Número de documento actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = VentaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Venta no existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"Venta no encontrada\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            ))),
            @ApiResponse(responseCode = "409", description = "Número de documento no disponible",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExistsRegisterException.class),
                    examples = @ExampleObject(
                            value = "{\"error\": \"El número de documento B123 ya está asignado a otra venta.\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 409}"
                    ))),
            @ApiResponse(responseCode = "500", description = "Validación número de documento no vacío",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusinessException.class),
                            examples = @ExampleObject(
                                    value = "{\"error\": \"El número de documento no puede estar vacío.\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 409}"
                            )))
    })
    @PatchMapping("/{id}/documento")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<VentaResponse> actualizarDocumentoVenta(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarDocumentoRequest request) {
        VentaResponse ventaActualizada = ventaService.actualizarDocumento(id, request);
        return ResponseEntity.ok(ventaActualizada);
    }

    @Operation(summary = "Actualizar cliente de venta", description = "Cambia o asigna el cliente de una venta existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente asociado a venta actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = VentaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Venta o Cliente no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"Venta no encontrada\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )))
    })
    @PatchMapping("/{id}/cliente")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<VentaResponse> actualizarClienteVenta(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarClienteRequest request) {
        VentaResponse ventaActualizada = ventaService.actualizarCliente(id, request);
        return ResponseEntity.ok(ventaActualizada);
    }

    @Operation(summary = "Listar ventas paginadas y filtradas", description = "Devuelve una lista paginada de ventas aplicando filtros dinámicos.")
    @ApiResponse(responseCode = "200", description = "Paginación de Ventas obtenida correctamente",
            content =  @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @PostMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<VentaResponse>> getMovimientosFiltrados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fecha") String sortBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @Valid @RequestBody VentaFilterDTO dto) {

        return ResponseEntity.ok(ventaService.getVentasFiltradas(page, size, sortBy, fechaInicio, fechaFin, dto));
    }


    @Operation(summary = "Obtener venta por ID", description = "Devuelve la venta correspondiente al ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Venta no existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"No existe Venta con id 1\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<VentaResponse> obtenerVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerVenta(id));
    }

    @Operation(summary = "Listar todas las ventas", description = "Obtiene una lista completa de ventas.")
    @ApiResponse(responseCode = "200", description = "Lista de ventas obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = VentaResponse.class))))
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<VentaResponse>> listarVentas() {
        return ResponseEntity.ok(ventaService.listarVentas());
    }

    @Operation(summary = "Eliminar venta", description = "Elimina una venta por ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Venta eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Venta no existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"Venta no encontrada\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            ))),
            @ApiResponse(responseCode = "500", description = "Validación stock",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusinessException.class),
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Stock insuficiente para realizar la salida manual.\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 409}"
                            )))
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarVenta(@PathVariable Long id, @Valid @RequestParam Long usuarioId) {
        ventaService.eliminarVenta(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener ganancias del mes actual", description = "Devuelve las ganancias del mes actual.")
    @ApiResponse(responseCode = "200", description = "Cálculo exitoso. En caso de no existir ganancias, retorna 0.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(description = "Mapa con la clave 'gananciaMesActual'", example = "{\"gananciaMesActual\": 1500000.0}"),
                    examples = @ExampleObject(name = "Ejemplo Ganancia Mes", summary = "Ganancia total del mes",
                            value = "{\"gananciaMesActual\": 4500000.0}"
                    )))
    @GetMapping("/ganancias/mes-actual")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<Map<String, Double>> obtenerGananciasDelMesActual() {
        Double ganancia = ventaService.getGananciasDelMesActual();
        Map<String, Double> response = Map.of("gananciaMesActual", ganancia);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener ganancias del día actual", description = "Devuelve las ganancias del día actual.")
    @ApiResponse(responseCode = "200", description = "Cálculo exitoso. En caso de no existir ganancias, retorna 0.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(description = "Mapa con la clave 'gananciaDiaActual'", example = "{\"gananciaDiaActual\": 1500000.0}"),
                    examples = @ExampleObject(name = "Ejemplo Ganancia Día", summary = "Ganancia total del día",
                            value = "{\"gananciaDiaActual\": 4500000.0}"
                    )))
    @GetMapping("/ganancias/mes-dia")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<Map<String, Double>> obtenerGananciasDelDiaActual() {
        Double ganancia = ventaService.getGananciasDelDiaActual();
        Map<String, Double> response = Map.of("gananciaDiaActual", ganancia);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Exportar Ventas a CSV", description = "Genera y descarga un archivo .csv con todos las ventas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo generado exitosamente", content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "500", description = "Error interno al generar el archivo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExportException.class),
                            examples = @ExampleObject(summary = "Error al exportar CSV", value = "{\"error\": \"Error al generar CSV de ventas.\", \"status\": 500}")))
    })
    @GetMapping("/exportar-csv")
    public void exportarVentas(
            @RequestParam(name = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @ModelAttribute VentaFilterDTO filtros,
            HttpServletResponse response) throws IOException {

        ZonedDateTime inicio = DateUtils.obtenerInicioDiaSegunFecha(fechaInicio);
        ZonedDateTime fin = DateUtils.obtenerFinDiaSegunFecha(fechaFin);

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Content-Disposition", "attachment; filename=\"reporte_ventas_decoaromas.csv\"");

        ventaExportService.escribirVentasACsv(response.getWriter(), inicio, fin, filtros);
    }
}
