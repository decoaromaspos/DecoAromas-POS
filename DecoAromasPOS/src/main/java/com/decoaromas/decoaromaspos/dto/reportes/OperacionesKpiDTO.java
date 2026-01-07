package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperacionesKpiDTO {

    private Double totalDescuadradoNeto;
    private Double totalDescuadradoAbsoluto;
    private Long numCajasDescuadre;
}