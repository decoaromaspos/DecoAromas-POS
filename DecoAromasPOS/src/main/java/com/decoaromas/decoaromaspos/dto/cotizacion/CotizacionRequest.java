package com.decoaromas.decoaromaspos.dto.cotizacion;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CotizacionRequest {
    private TipoCliente tipoCliente;

    private Double valorDescuentoGlobal;
    private TipoDescuento tipoDescuentoGlobal;

    private Long usuarioId;
    private Long clienteId;    // opcional
    private List<DetalleCotizacionRequest> detalles;
}
