package com.decoaromas.decoaromaspos.utils;

import java.util.function.Supplier;

import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityChecker {

    /**
     * Método genérico para verificar la disponibilidad.
     * Recibe una función (Supplier) que ejecuta la consulta de existencia en la DB.
     *
     * @param existsSupplier La función de consulta a la DB.
     * @param fieldName Nombre del campo (ej: "RUT", "Nombre").
     * @param fieldValue Valor del campo.
     * @return AvailabilityResponse con el resultado.
     */
    public AvailabilityResponse check(
            Supplier<Boolean> existsSupplier,
            String fieldName,
            String fieldValue) {

        boolean exists = existsSupplier.get();

        if (exists) {
            return new AvailabilityResponse(
                    false,
                    fieldName + " '" + fieldValue + "' ya en uso. Ingrese otro."
            );
        } else {
            return new AvailabilityResponse(
                    true,
                    fieldName + " disponible."
            );
        }
    }
}