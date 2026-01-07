package com.decoaromas.decoaromaspos.model;

import com.decoaromas.decoaromaspos.enums.TipoDescuento;
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
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detalleId;

    private Integer cantidad;
    private Double precioUnitario;


    private Double valorDescuentoUnitario;

    @Enumerated(EnumType.STRING)
    private TipoDescuento tipoDescuentoUnitario;

    private Double subtotalBruto; // Guarda el (precioUnitario * cantidad). Es el subtotal "Bruto" de la línea.

    /**
     * NUEVO CAMPO: Guarda el monto total (en dinero) del descuento aplicado a esta línea.
     * (Ej.: $1.000 de descuento por unidad * 2 unidades = $2.000)
     */
    private Double montoDescuentoUnitarioCalculado;

    /**
     * Guarda el total final de la línea.
     * (subtotalBruto - montoDescuentoUnitarioCalculado)
     */
    private Double subtotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productoId")
    private Producto producto;

    private String codigoBarras;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ventaId")
    private Venta venta;
}
