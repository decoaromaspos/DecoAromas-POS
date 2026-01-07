package com.decoaromas.decoaromaspos.dto.other.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Configuración de conexión para la impresora")
public class PrinterRequest {

    @Schema(description = "Dirección IP de la impresora (opcional si se usa configuración global)", example = "192.0.2.10")
    private String ip;          // IP de la impresora o del host que comparte la impresora

    @Schema(description = "Puerto de comunicación (Por defecto 9100)", example = "9100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Integer port = 9100; // puerto (por defecto 9100)

    @Schema(
            description = "Líneas de texto a imprimir. Nota: En el endpoint de ventas, este campo es ignorado ya que se genera automáticamente, pero es requerido por validación si se envía el cuerpo.",
            example = "[\"Linea 1\", \"Linea 2\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty
    private List<String> lines; // líneas del comprobante, en orden
}
