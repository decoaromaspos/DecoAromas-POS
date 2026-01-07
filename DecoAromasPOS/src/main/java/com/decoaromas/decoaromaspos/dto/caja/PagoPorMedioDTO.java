package com.decoaromas.decoaromaspos.dto.caja;

import com.decoaromas.decoaromaspos.enums.MedioPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilizado para mapear los resultados de la consulta de agregación
 * que suma los pagos por medio de pago.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoPorMedioDTO {
    private MedioPago medioPago;
    private Double total;
}