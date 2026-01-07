package com.decoaromas.decoaromaspos.dto.reportes;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;


@Data
public class ReporteGeneralFiltroDTO {

    /**
     * Fecha de inicio del rango. Obligatoria.
     * Espera el formato AAAA-MM-DD (ISO DATE).
     */
    @NotNull(message = "La fecha de inicio no puede ser nula")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaInicio;

    /**
     * Fecha de fin del rango. Obligatoria.
     * Espera el formato AAAA-MM-DD (ISO DATE).
     */
    @NotNull(message = "La fecha de fin no puede ser nula")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;

    private TipoCliente tipoCliente; // Spring convertirá el String a Enum
    private Long familiaId;
    private Long aromaId;
}