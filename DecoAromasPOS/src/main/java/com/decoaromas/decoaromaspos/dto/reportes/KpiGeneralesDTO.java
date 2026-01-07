package com.decoaromas.decoaromaspos.dto.reportes;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiGeneralesDTO {
    // De Venta
    private Double totalVentasNetas; // Tienda física
    private Double totalVentasOnline; // Nuevo KPI
    private Double utilidadNeta;
    private Long totalTransacciones;
    private Double ticketPromedio;

    // De Caja
    private Double descuadreNetoTotal;
}