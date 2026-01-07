package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalisisDescuentoDTO {
    private Integer mes;
    private Double totalVendido;
    private Double totalDescuento;
}