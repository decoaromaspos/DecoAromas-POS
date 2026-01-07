package com.decoaromas.decoaromaspos.dto.other.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Estructura para actualizar un valor de configuración")
public class ConfigValueRequest {

    @Schema(description = "El nuevo valor a establecer", example = "1500000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String valor;
}