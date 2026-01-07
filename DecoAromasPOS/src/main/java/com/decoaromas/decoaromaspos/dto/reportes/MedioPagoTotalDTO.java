package com.decoaromas.decoaromaspos.dto.reportes;

import com.decoaromas.decoaromaspos.enums.MedioPago;

public interface MedioPagoTotalDTO {
    MedioPago getMedioPago();
    Double getTotal();
}