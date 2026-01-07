package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.enums.MedioPago;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoResponse {
    private MedioPago medioPago;
    private Double monto;
}