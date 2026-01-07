package com.decoaromas.decoaromaspos.dto.cotizacion;

import com.decoaromas.decoaromaspos.enums.EstadoCotizacion;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CotizacionUpdateEstado {
    @NotNull(message = "Debe ingresar un id de cotizacion")
    private Long cotizacionId;

    @NotNull(message = "Debe seleccionar un tipo de estado (PENDIENTE, APROBADA, RECHAZADA, CONVERTIDA)")
    private EstadoCotizacion estado;
}
