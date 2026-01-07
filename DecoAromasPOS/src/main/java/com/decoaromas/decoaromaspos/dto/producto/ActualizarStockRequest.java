package com.decoaromas.decoaromaspos.dto.producto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ActualizarStockRequest {

    @NotNull(message = "Debe ingresar una cantidad nueva de stock")
    private Integer nuevaCantidad;

    @NotNull(message = "Debe asociar un usuario")
    @Positive(message = "La id ser positiva")
    private Long usuarioId;
}