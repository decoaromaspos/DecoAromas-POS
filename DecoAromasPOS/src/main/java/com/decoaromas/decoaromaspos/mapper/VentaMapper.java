package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.venta.DetalleVentaResponse;
import com.decoaromas.decoaromaspos.dto.venta.PagoResponse;
import com.decoaromas.decoaromaspos.dto.venta.VentaResponse;
import com.decoaromas.decoaromaspos.model.DetalleVenta;
import com.decoaromas.decoaromaspos.model.Venta;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VentaMapper {

    public VentaResponse toResponse(Venta venta) {
        VentaResponse response = new VentaResponse();
        response.setVentaId(venta.getVentaId());
        response.setFecha(venta.getFecha());
        response.setTipoCliente(venta.getTipoCliente());

        // --- Mapeo de Totales ---
        response.setTotalBruto(venta.getTotalBruto());

        response.setValorDescuentoGlobal(venta.getValorDescuentoGlobal());
        response.setTipoDescuentoGlobal(venta.getTipoDescuentoGlobal());
        response.setMontoDescuentoGlobalCalculado(venta.getMontoDescuentoGlobalCalculado());

        response.setTotalDescuentosUnitarios(venta.getTotalDescuentosUnitarios());
        response.setTotalDescuentoTotal(venta.getTotalDescuentoTotal());
        response.setTotalNeto(venta.getTotalNeto());

        response.setTipoDocumento(venta.getTipoDocumento());
        response.setNumeroDocumento(venta.getNumeroDocumento());
        response.setCostoGeneral(venta.getCostoGeneral());
        response.setVuelto(venta.getVuelto());
        response.setCajaId(venta.getCaja().getCajaId());

        response.setUsuarioId(venta.getUsuario().getUsuarioId());
        if (venta.getUsuario().getApellido() == null) {
            response.setUsuarioNombre(venta.getUsuario().getNombre());
        } else {
            response.setUsuarioNombre(venta.getUsuario().getNombre() + " " + venta.getUsuario().getApellido());
        }

        if (venta.getCliente() != null) {
            response.setClienteId(venta.getCliente().getClienteId());

            if (venta.getCliente().getApellido() == null) {
                response.setClienteNombre(venta.getCliente().getNombre());
            } else {
                response.setClienteNombre(venta.getCliente().getNombre() + " " + venta.getCliente().getApellido());
            }
        }

        List<DetalleVentaResponse> detalles = venta.getDetalles().stream()
                .map(this::toDetalleVentaResponse).toList();

        response.setDetalles(detalles);


        // Mapear lista de pagos de la entidad a la lista de DTOs de respuesta.
        List<PagoResponse> pagosResponse = venta.getPagos().stream()
                .map(pago -> PagoResponse.builder()
                        .medioPago(pago.getMedioPago())
                        .monto(pago.getMonto())
                        .build())
                .toList();
        response.setPagos(pagosResponse);

        return response;
    }

    // Helper privado para crear la respuesta de detalle de venta
    private DetalleVentaResponse toDetalleVentaResponse(DetalleVenta det) {
        DetalleVentaResponse d = new DetalleVentaResponse();
        d.setDetalleId(det.getDetalleId());
        d.setCodigoBarras(det.getCodigoBarras());
        d.setProductoId(det.getProducto().getProductoId());
        d.setProductoNombre(det.getProducto().getNombre());
        d.setCantidad(det.getCantidad());
        d.setPrecioUnitario(det.getPrecioUnitario());
        d.setValorDescuentoUnitario(det.getValorDescuentoUnitario());
        d.setTipoDescuentoUnitario(det.getTipoDescuentoUnitario());

        d.setSubtotalBruto(det.getSubtotalBruto());
        d.setMontoDescuentoUnitarioCalculado(det.getMontoDescuentoUnitarioCalculado());
        d.setSubtotal(det.getSubtotal()); // (Este es el subtotal NETO de la línea)

        return d;
    }
}
