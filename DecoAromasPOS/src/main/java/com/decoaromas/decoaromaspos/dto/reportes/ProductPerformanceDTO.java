package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPerformanceDTO {
    private String nombreProducto;
    private Long volumen;          // Total de unidades vendidas (Eje X)
    private Double rentabilidad;   // Total de ganancia (Ingresos - Costos) (Eje Y)
}
