package com.decoaromas.decoaromaspos.dto.producto;

import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MovimientoStockRequest {

    @NotNull(message = "Debe ingresar un stock asociado al movimiento")
    private Integer cantidad;
    
    private TipoMovimiento tipo;

    private MotivoMovimiento motivo;

    @NotNull(message = "Debe asociar un usuario")
    @Positive(message = "La id ser positiva")
    private Long usuarioId;

}