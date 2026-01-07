package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.enums.MedioPago;
import lombok.Data;

@Data
public class PagoRequest {
    private MedioPago medioPago;
    private Double monto;
}