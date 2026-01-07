package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaAgrupadaPorNombreDTO {
    private String nombre; // Para el nombre del día o el nombre del vendedor
    private Double total;  // Para el total de ventas
}