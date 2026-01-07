package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaResponse {
    private Long ventaId;
    private ZonedDateTime fecha;
    private TipoCliente tipoCliente;
    private Double totalBruto;  // Suma de (precio * cant) de todos los detalles. ANTES de descuentos.

    private Double valorDescuentoGlobal;      // El valor numérico (ej: 15.0 o 500.0)
    private TipoDescuento tipoDescuentoGlobal;  // VALOR o PORCENTAJE
    private Double montoDescuentoGlobalCalculado;

    /**
     * Suma de todos los descuentos unitarios (los 'montoDescuentoUnitarioCalculado').
     */
    private Double totalDescuentosUnitarios;

    /**
     * Suma total de descuentos (totalDescuentosUnitarios + montoDescuentoGlobalCalculado).
     */
    private Double totalDescuentoTotal;

    private Double totalNeto;  // Total final a pagar (totalBruto - totalDescuentoTotal).

    private Double vuelto;
    private Double costoGeneral;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;

    private Long usuarioId;
    private String usuarioNombre;
    private Long cajaId;
    private Long clienteId;
    private String clienteNombre;
    private List<DetalleVentaResponse> detalles;
    private List<PagoResponse> pagos;
}
