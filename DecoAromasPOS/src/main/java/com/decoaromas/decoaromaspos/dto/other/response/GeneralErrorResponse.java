package com.decoaromas.decoaromaspos.dto.other.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "Estructura estándar de error de la API")
public class GeneralErrorResponse {
    @Schema(description = "Fecha y hora del error", example = "2023-11-29T10:15:30")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP", example = "409")
    private int status;

    @Schema(description = "Mensaje descriptivo del error", example = "Ya existe un aroma con nombre Vainilla.")
    private String error;
}