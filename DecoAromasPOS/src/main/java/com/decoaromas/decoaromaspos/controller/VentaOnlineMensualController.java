package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.dto.venta_online_mensual.VentaOnlineMensualRequest;
import com.decoaromas.decoaromaspos.dto.venta_online_mensual.VentaOnlineMensualResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.VentaOnlineMensualService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventasonline/mensuales")
@RequiredArgsConstructor
@Tag(name = "Gestión de Ventas Online Mensuales", description = "API para administrar el registro consolidado de ventas online por mes y año.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(summary = "Usuario no autenticado",
                                value = "{\"path\": \"/api/ventasonline/mensuales\", " +
                                        "\"error\": \"No autorizado\", " +
                                        "\"message\": \"Se requiere autenticación para acceder a este recurso. El token puede ser inválido o haber expirado.\", " +
                                        "\"status\": 401}"
                        )
                )
        )
})
public class VentaOnlineMensualController {

    private final VentaOnlineMensualService ventaOnlineMensualService;

    // Listar Todas
    @Operation(summary = "Listar todas las ventas online mensuales", description = "Obtiene una lista completa de ventas online mensuales.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = VentaOnlineMensualResponse.class))))
    @GetMapping
    public ResponseEntity<List<VentaOnlineMensualResponse>> listarVentasOnlineMensual() {
        return ResponseEntity.ok(ventaOnlineMensualService.listarVentasOnlineMensuales());
    }

    // Obtener por ID
    @Operation(summary = "Obtener venta online mensual por ID", description = "Devuelve la venta online mensual correspondiente al ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaOnlineMensualResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(summary = "Venta online mensual no encontrada",
                                    value = "{\"error\": \"No existe venta online mensual con id 1\", " +
                                            "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                            "\"status\": 404}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<VentaOnlineMensualResponse> obtenerVentaOnlineMensualPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ventaOnlineMensualService.obtenerVentaOnlineMensualPorId(id));
    }

    // Obtener por año y mes
    @Operation(summary = "Obtener venta online mensual por año y mes", description = "Devuelve la venta online mensual correspondiente al año y mes especificados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaOnlineMensualResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe registro para esa fecha",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(summary = "Venta online mensual no encontrada según año y mes",
                                    value = "{\"error\": \"No se encontró registro para el mes 12 del año 2025\"," +
                                            "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                            "\"status\": 404}")))
    })
    @GetMapping("/anio/mes")
    public ResponseEntity<VentaOnlineMensualResponse> obtenerVentaOnlineMensualPorAnioMes(
            @RequestParam Integer anio,
            @RequestParam Integer mes) {
        return ResponseEntity.ok(ventaOnlineMensualService.obtenerVentaOnlineMensualPorAnioMes(anio, mes));
    }

    // Obtener por año (lista)
    @Operation(summary = "Obtener ventas online mensuales por año", description = "Devuelve todos los registros mensuales pertenecientes a un año específico.")
    @ApiResponse(responseCode = "200", description = "Lista del año obtenida",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = VentaOnlineMensualResponse.class))))
    @GetMapping("/anio/{anio}")
    public ResponseEntity<List<VentaOnlineMensualResponse>> obtenerPorAnio(@PathVariable Integer anio) {
        return ResponseEntity.ok(ventaOnlineMensualService.obtenerVentasOnlineMensualesPorAnio(anio));
    }

    // Crear
    @Operation(summary = "Crear venta online mensual", description = "Crea una nueva venta online mensual. Valida que no sea fecha futura y que no exista ya el registro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaOnlineMensualResponse.class))),

            @ApiResponse(responseCode = "400", description = "Datos inválidos o fecha futura",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IllegalArgumentException.class),
                            examples = @ExampleObject(summary = "Error de fecha futura",
                                    value = "{\"error\": \"No se pueden registrar ventas para un mes futuro (2030-1)\", \"timestamp\": \"2025-12-09T18:47:24.763249103}\", \"status\": 400}"))),

            @ApiResponse(responseCode = "409", description = "Registro duplicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExistsRegisterException.class),
                            examples = @ExampleObject(summary = "Ya existe mes/año",
                                    value = "{\"error\": \"Ya existe registro para el mes 5 y año 2024\", \"timestamp\": \"2025-12-09T18:47:24.763249103}\", \"status\": 409}")))
    })
    @PostMapping
    public ResponseEntity<VentaOnlineMensualResponse> crearVentaOnlineMensual(@Valid @RequestBody VentaOnlineMensualRequest request) {
        return new ResponseEntity<>(ventaOnlineMensualService.crearVentaOnlineMensual(request), HttpStatus.CREATED);
    }

    // Actualizar según año y mes
    @Operation(summary = "Actualizar venta online mensual", description = "Actualiza los datos de una venta online mensual existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VentaOnlineMensualResponse.class))),

            @ApiResponse(responseCode = "404", description = "No se encontró el registro a actualizar (Año/Mes inexistente)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(summary = "Venta online mensual no encontrada según año y mes",
                                    value = "{\"error\": \"No se encontró registro para el mes 12 del año 2025\"," +
                                            "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                            "\"status\": 404}"))),

            @ApiResponse(responseCode = "400", description = "Datos inválidos (negativos o fechas incorrectas)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MethodArgumentNotValidException.class))),
    })
    @PutMapping("/update")
    public ResponseEntity<VentaOnlineMensualResponse> actualizarVentaOnlineMensual(@Valid @RequestBody VentaOnlineMensualRequest request) {
        return ResponseEntity.ok(ventaOnlineMensualService.actualizarVentaOnlineMensual(request));
    }

    // Eliminar según año y mes
    @Operation(summary = "Eliminar venta online mensual", description = "Elimina una venta online mensual por ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                        examples = @ExampleObject(summary = "Venta online mensual no encontrada",
                                value = "{\"error\": \"No existe venta online mensual con id 1645654\", " +
                                        "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                        "\"status\": 404}")))
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> eliminarVentaOnline(@PathVariable Long id) {
        ventaOnlineMensualService.eliminarVentaOnlineMensual(id);
        return ResponseEntity.noContent().build();
    }
}
