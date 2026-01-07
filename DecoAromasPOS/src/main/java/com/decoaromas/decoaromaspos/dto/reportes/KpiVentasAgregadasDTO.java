package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpiVentasAgregadasDTO {
    private Double totalVentasNetas;
    private Double utilidadNeta;
    private Long totalTransacciones;
}
