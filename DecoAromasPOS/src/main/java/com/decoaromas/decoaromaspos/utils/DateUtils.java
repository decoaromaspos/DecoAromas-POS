package com.decoaromas.decoaromaspos.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@UtilityClass
public class DateUtils {

    public static final ZoneId ZONE_ID_SANTIAGO = ZoneId.of("America/Santiago");


    // Permite obtener el tiempo exacto (hora/segundos) donde inicia un día LocalDate
    public static ZonedDateTime obtenerInicioDiaSegunFecha(LocalDate fechaInicio) {
        if (fechaInicio == null) {
            return null;
        }
        return fechaInicio.atStartOfDay(ZONE_ID_SANTIAGO);
    }

    // Permite obtener el tiempo exacto (hora/segundos) donde termina un día LocalDate
    public static ZonedDateTime obtenerFinDiaSegunFecha(LocalDate fechaFin) {
        if (fechaFin == null) {
            return null;
        }
        return fechaFin.atTime(23, 59, 59, 999_999_999).atZone(ZONE_ID_SANTIAGO);
    }

    public static ZonedDateTime obtenerFechaHoraActual() {
        return ZonedDateTime.now(ZONE_ID_SANTIAGO);
    }

    public static LocalDate obtenerFechaActual() {
        return LocalDate.now(ZONE_ID_SANTIAGO);
    }
}
