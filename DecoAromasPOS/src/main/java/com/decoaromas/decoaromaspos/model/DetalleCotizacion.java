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
public class DetalleCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detalleCotizacionId;

    private Integer cantidad;
    private Double precioUnitario; // El precio al momento de cotizar

    private Double valorDescuentoUnitario;
    @Enumerated(EnumType.STRING)
    private TipoDescuento tipoDescuentoUnitario;

    private Double subtotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productoId")
    private Producto producto;

    private String codigoBarras;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacionId")
    private Cotizacion cotizacion;
}