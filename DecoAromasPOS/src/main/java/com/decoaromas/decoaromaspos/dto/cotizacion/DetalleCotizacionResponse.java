package com.decoaromas.decoaromaspos.dto.cotizacion;

import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleCotizacionResponse {
    private Long detalleCotizacionId;
    private Long productoId;
    private String codigoBarras;
    private String productoNombre;
    private Integer cantidad;
    private Double precioUnitario;

    private Double valorDescuentoUnitario;      // El valor numérico (ej: 10.0 o 100.0)
    private TipoDescuento tipoDescuentoUnitario;  // VALOR o PORCENTAJE

    private Double subtotal;
}
