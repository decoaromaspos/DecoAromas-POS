package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.model.PagoVenta;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// --- DTO Helper ---
@Data
@AllArgsConstructor
public class DatosPagoProcesado {
    private List<PagoVenta> pagosEntidad;
    private double vuelto;
}