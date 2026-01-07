package com.decoaromas.decoaromaspos.dto.other.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeletedIdRequest {

    @NotNull(message = "El id es obligatorio")
    @Positive(message = "El id debe ser positivo")
    private Long id;

    @NotNull(message = "El Boolean deleted es obligatorio")
    private Boolean deleted;
}
