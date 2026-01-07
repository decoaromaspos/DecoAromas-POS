package com.decoaromas.decoaromaspos.dto.caja;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Resumen de totales acumulados por medio de pago")
public class CajaResumenResponse {
    @Schema(description = "Total efectivo neto (Recibido - Vueltos)", example = "150000.0")
    private Double totalEfectivo;

    @Schema(description = "Total Mercado Pago", example = "50000.0")
    private Double totalMercadoPago;

    @Schema(description = "Total Tarjetas (BCI)", example = "200000.0")
    private Double totalBCI;

    @Schema(description = "Total Botón de Pago (Web)", example = "0.0")
    private Double totalBotonDePago;

    @Schema(description = "Total Transferencias", example = "75000.0")
    private Double totalTransferencia;

    @Schema(description = "Total Ventas vía Post", example = "45000.0")
    private Double totalPost;
}
