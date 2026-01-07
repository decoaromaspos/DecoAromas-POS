package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class VentaRequest {
    private TipoCliente tipoCliente;

    private Double valorDescuentoGlobal;
    private TipoDescuento tipoDescuentoGlobal;

    private TipoDocumento tipoDocumento;
    private Long usuarioId;
    private Long clienteId;    // opcional
    private List<DetalleVentaRequest> detalles;

    private List<PagoRequest> pagos;

    private Long cotizacionId;   // opcional
}
