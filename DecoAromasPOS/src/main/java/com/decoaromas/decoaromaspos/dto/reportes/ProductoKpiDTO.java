package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductoKpiDTO {
    private String productoEstrella; // Nombre del producto más vendido
    private String aromaMasPopular;
    private String familiaMasPopular;
    private String productoMenosVendido;
}
