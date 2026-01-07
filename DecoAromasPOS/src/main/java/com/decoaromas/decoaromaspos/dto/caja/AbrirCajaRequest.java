package com.decoaromas.decoaromaspos.dto.caja;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Datos requeridos para iniciar un turno de caja")
public class AbrirCajaRequest {

    @Schema(description = "ID del usuario que abre la caja", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El id de usuario es obligatorio")
    @Positive
    private Long usuarioId;

    @Schema(description = "Monto de efectivo inicial en la caja (fondo fijo)", example = "20000.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Debe incluir efectivo de apertura de caja")
    @PositiveOrZero
    private Double efectivoApertura;
}
