package com.decoaromas.decoaromaspos.dto.movimiento_inventario;

import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CrearMovimientoInventarioRequest {
    @NotBlank(message = "Debe ingresar un producto")
    private Long productoId;
    @NotBlank(message = "Debe ingresar una cantidad de producto en el movimiento")
    private Integer cantidad;
    @NotBlank(message = "Debe ingresar un usuario")
    private Long usuarioId;
    @NotNull(message = "Debe seleccionar el tipo de movimiento (ENTRADA, SALIDA)")
    private TipoMovimiento tipo;
    @NotNull(message = "Debe seleccionar un tipo de movimiento (VENTA, COMPRA, CORRECION)")
    private MotivoMovimiento motivo;
}
