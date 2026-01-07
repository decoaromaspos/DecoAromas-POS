package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVentaResponse {
    private Long detalleId;
    private Long productoId;
    private String codigoBarras;
    private String productoNombre;
    private Integer cantidad;
    private Double precioUnitario;

    private Double valorDescuentoUnitario;      // El valor numérico (ej: 10.0 o 100.0)
    private TipoDescuento tipoDescuentoUnitario;  // VALOR o PORCENTAJE

    /**
     * El subtotal bruto de la línea (precioUnitario * cantidad). ANTES de descuentos.
     */
    private Double subtotalBruto;

    /**
     * El monto total (en dinero) del descuento aplicado a ESTA LÍNEA. (Ej.: $1.000 de dcto * 2 unidades = $2.000)
     */
    private Double montoDescuentoUnitarioCalculado;

    /**
     * El subtotal NETO de la línea (subtotalBruto - montoDescuentoUnitarioCalculado).
     * Este es el valor que se muestra en la columna "Monto" del ticket.
     */
    private Double subtotal;
}
