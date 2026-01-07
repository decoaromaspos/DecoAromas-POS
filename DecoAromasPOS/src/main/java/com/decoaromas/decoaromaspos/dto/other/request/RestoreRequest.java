package com.decoaromas.decoaromaspos.dto.other.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para recibir el nombre del archivo de backup
 * que el usuario selecciona desde el frontend.
 */
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Solicitud para restaurar una base de datos desde un archivo específico")
public class RestoreRequest {

    @Schema(
            description = "Nombre exacto del archivo .dump ubicado en el volumen de backups",
            example = "decoaromas_2025-11-29_18-30.dump",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String filename;
}