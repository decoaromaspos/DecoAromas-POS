package com.decoaromas.decoaromaspos.model;

import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ventaId;

    private ZonedDateTime fecha;

    @Enumerated(EnumType.STRING)
    private TipoCliente tipoCliente;

    @BatchSize(size = 20)
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoVenta> pagos = new ArrayList<>();

    private Double totalBruto; // Suma de (precio * cant) de detalles.


    private Double valorDescuentoGlobal;
    @Enumerated(EnumType.STRING)
    private TipoDescuento tipoDescuentoGlobal;
    private Double montoDescuentoGlobalCalculado;   // Guarda el monto final del descuento en dinero.

    private Double totalDescuentosUnitarios; // Guarda la suma de todos los descuentos unitarios (los de DetalleVenta).
    private Double totalDescuentoTotal; // Guarda la suma total de descuentos (unitarios + global).

    private Double totalNeto;  // Total a pagar (totalBruto - totalDescuentoTotal)
    private TipoDocumento tipoDocumento;
    @Column(nullable = true, unique = true, length = 30)
    private String numeroDocumento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarioId")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cajaId")
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "clienteId", nullable = true)
    private Cliente cliente;            // Restricción, si puede existir una venta sin cliente

    // Relación con detalles
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    private Double costoGeneral;
    private Double vuelto = 0.0;




    // Métodos
    public void addDetalle(DetalleVenta detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }

    public void removeDetalle(DetalleVenta detalle) {
        detalles.remove(detalle);
        detalle.setVenta(null);
    }

    public void addPago(PagoVenta pago) {
        pagos.add(pago);
        pago.setVenta(this);
    }

    public void removePago(PagoVenta pago) {
        pagos.remove(pago);
        pago.setVenta(null);
    }
}
