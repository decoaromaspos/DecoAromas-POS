package com.decoaromas.decoaromaspos.dto.other.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Schema(description = "Estructura de error para fallos de validación")
public class ValidationErrorResponse {
    @Schema(description = "Fecha y hora del error", example = "2023-11-29T10:15:30")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP", example = "400")
    private int status;

    @Schema(description = "Descripción general", example = "Error de validación")
    private String error;

    @Schema(description = "Mapa de campos y sus mensajes de error", example = "{\"nombre\": \"El nombre es obligatorio\"}")
    private Map<String, String> details;
}