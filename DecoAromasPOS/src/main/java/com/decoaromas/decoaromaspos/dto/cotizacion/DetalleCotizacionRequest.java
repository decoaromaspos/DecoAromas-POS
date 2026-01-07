package com.decoaromas.decoaromaspos.dto.cotizacion;

import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DetalleCotizacionRequest {
    private Long productoId;
    private Integer cantidad;
    private Double valorDescuentoUnitario;
    private TipoDescuento tipoDescuentoUnitario;
}
