package com.decoaromas.decoaromaspos.dto.other.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Respuesta de error de autenticación (401)")
public class UnauthorizedResponse {
    @Schema(example = "/api/aromas")
    private String path;

    @Schema(example = "No autorizado")
    private String error;

    @Schema(example = "Se requiere autenticación para acceder a este recurso.")
    private String message;

    @Schema(example = "401")
    private int status;
}