package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiVentasDTO {

    // Núm. de Transacciones: (Conteo de Venta)
    private Long totalTransacciones;

    // Total Descuento: (Suma de montoDescuentoGlobalCalculado)
    private Double totalDescuentos;

    // Ticket Promedio: (Total Ventas / Número de Ventas)
    private Double ticketPromedio;

    // Incluimos este que se calcula de todas formas
    private Double totalVentasNetas;

    private Double totalVentasOnline;


    // Constructor manual para el Repository (4 campos)
    public KpiVentasDTO(Long totalTransacciones, Double totalDescuentos, Double ticketPromedio, Double totalVentasNetas) {
        this.totalTransacciones = totalTransacciones;
        this.totalDescuentos = totalDescuentos;
        this.ticketPromedio = ticketPromedio;
        this.totalVentasNetas = totalVentasNetas;
        this.totalVentasOnline = 0.0; // Valor por defecto
    }
}