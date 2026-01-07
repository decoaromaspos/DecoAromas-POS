package com.decoaromas.decoaromaspos.dto.producto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponse {

    private Long productoId;
    private String nombre;
    private String descripcion;
    private String sku;
    private String codigoBarras;
    private Double precioDetalle;
    private Double precioMayorista;
    private Integer stock;
    private Double costo;
    private Long familiaId;
    private String familiaNombre;
    private Long aromaId;
    private String aromaNombre;
    private Boolean activo;
}