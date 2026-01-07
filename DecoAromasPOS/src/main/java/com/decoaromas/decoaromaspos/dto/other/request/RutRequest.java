package com.decoaromas.decoaromaspos.dto.other.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RutRequest {

    @NotBlank(message = "Debe ingresar un RUT")
    @Size(min = 9, max = 10, message = "El RUT debe tener entre 9 y 10 caracteres")
    @Pattern(
            regexp = "^\\d+-[\\dkK]$",
            message = "El formato del RUT no es válido")
    private String rut;
}
