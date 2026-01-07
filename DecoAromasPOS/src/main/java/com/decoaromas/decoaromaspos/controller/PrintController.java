package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.request.PrinterRequest;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.PrinterService;
import com.decoaromas.decoaromaspos.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.IS_AUTHENTICATED;

@RestController
@RequestMapping("/api/print")
@RequiredArgsConstructor
@Validated
@Tag(name = "Impresión de Tickets", description = "API para enviar comandos de impresión de comprobantes de venta a impresoras térmica de red (POS).")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/print\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class PrintController {

    private final PrinterService printerService;
    private final VentaService ventaService;

    @Operation(summary = "Imprimir comprobante de venta",
            description = "Genera el ticket formateado de una venta existente y lo envía a la impresora configurada. " +
                    "Si no se envía cuerpo de petición, usa el puerto por defecto (9100) y la IP configurada en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comando de impresión enviado correctamente",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Comprobante impreso para venta 1540"))),

            @ApiResponse(responseCode = "404", description = "Venta no encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(summary = "Venta no encontrada según ID",
                                    value = "{\"error\": \"No existe venta con id 23\"," +
                                            "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                            "\"status\": 404}"))),

            @ApiResponse(responseCode = "500", description = "Error de conexión con la impresora",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Error al imprimir: Connection refused: connect")))
    })
    @PostMapping("/imprimir/{ventaId}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<String> printReceiptFromSale(
            @PathVariable Long ventaId,
            @RequestBody(required = false) PrinterRequest config) {

        // Obtener venta y construir líneas
        var venta = ventaService.obtenerVentaRealPorId(ventaId);
        List<String> lines = ventaService.buildReceiptLines(venta);
        int port = (config != null && config.getPort() != null) ? config.getPort() : 9100;

        try {
            printerService.printReceipt(port, lines);
            return ResponseEntity.status(201).body("Comprobante impreso para venta " + ventaId);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al imprimir: " + e.getMessage());
        }
    }

}
