package com.decoaromas.decoaromaspos.dto.reportes;

import java.time.LocalDate;

public interface VentaDiariaProjection {
    LocalDate getFecha();
    Double getTotal();
}