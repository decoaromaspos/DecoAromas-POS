package com.decoaromas.decoaromaspos.dto.familia;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FamiliaRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
}
