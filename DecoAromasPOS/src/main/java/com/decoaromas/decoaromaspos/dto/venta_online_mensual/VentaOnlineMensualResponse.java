package com.decoaromas.decoaromaspos.dto.venta_online_mensual;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaOnlineMensualResponse {
    private Long id;
    private Integer anio;
    private Integer mes;
    private Double totalDetalle;
    private Double totalMayorista;
}
