package com.decoaromas.decoaromaspos.dto.cotizacion;

import com.decoaromas.decoaromaspos.enums.EstadoCotizacion;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionResponse {
    private Long cotizacionId;
    private ZonedDateTime fechaEmision;
    private TipoCliente tipoCliente;
    private Double totalBruto;

    private Double valorDescuentoGlobal;      // El valor numérico (ej: 15.0 o 500.0)
    private TipoDescuento tipoDescuentoGlobal;  // VALOR o PORCENTAJE
    private Double montoDescuentoGlobalCalculado;

    private Double totalNeto;
    private Double costoGeneral;
    private EstadoCotizacion estado;

    private Long usuarioId;
    private String usuarioNombre;
    private Long clienteId;
    private String clienteNombre;
    private List<DetalleCotizacionResponse> detalles;
}