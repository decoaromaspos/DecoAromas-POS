package com.decoaromas.decoaromaspos.model;

import com.decoaromas.decoaromaspos.enums.EstadoCotizacion;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cotizacionId;

    private ZonedDateTime fechaEmision;

    @Enumerated(EnumType.STRING)
    private EstadoCotizacion estado; // Ej: PENDIENTE, APROBADA, RECHAZADA, CONVERTIDA

    @Enumerated(EnumType.STRING)
    private TipoCliente tipoCliente;

    private Double totalBruto;
    private Double valorDescuentoGlobal;
    @Enumerated(EnumType.STRING)
    private TipoDescuento tipoDescuentoGlobal;
    private Double montoDescuentoGlobalCalculado;

    private Double totalNeto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuarioId")
    private Usuario usuario; // Usuario que creó la cotización

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "clienteId", nullable = true)
    private Cliente cliente; // Cliente al que se le cotiza

    // Relación con detalles de cotización
    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCotizacion> detalles = new ArrayList<>();

    private Double costoGeneral; // Útil para ver la rentabilidad de la cotización




    // Métodos
    public void addDetalle(DetalleCotizacion detalle) {
        detalles.add(detalle);
        detalle.setCotizacion(this);
    }

    public void removeDetalle(DetalleCotizacion detalle) {
        detalles.remove(detalle);
        detalle.setCotizacion(null);
    }
}
