package com.decoaromas.decoaromaspos.dto.aroma;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AromaRequest {

    @Schema(
            description = "Nombre comercial del aroma",
            example = "Vainilla Francesa",
            requiredMode = Schema.RequiredMode.REQUIRED // Esto pone el asterisco rojo en la doc
    )
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
}