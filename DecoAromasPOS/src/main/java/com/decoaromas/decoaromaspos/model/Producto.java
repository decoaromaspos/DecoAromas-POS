package com.decoaromas.decoaromaspos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productoId;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = true, unique = true)
    private String codigoBarras;    // Inicialmente se puede nulear, despues no

    private Double precioDetalle;

    private Double precioMayorista;

    private Integer stock;

    private Double costo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familiaId")
    private FamiliaProducto familia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aromaId")
    private Aroma aroma;

    private Boolean activo = true;

}
