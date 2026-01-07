package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.cotizacion.CotizacionResponse;
import com.decoaromas.decoaromaspos.dto.cotizacion.DetalleCotizacionResponse;
import com.decoaromas.decoaromaspos.model.Cotizacion;
import com.decoaromas.decoaromaspos.model.DetalleCotizacion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CotizacionMapper {

    public CotizacionResponse toResponse(Cotizacion cotizacion) {
        CotizacionResponse response = new CotizacionResponse();
        response.setCotizacionId(cotizacion.getCotizacionId());
        response.setFechaEmision(cotizacion.getFechaEmision());
        response.setTipoCliente(cotizacion.getTipoCliente());
        response.setTotalBruto(cotizacion.getTotalBruto());

        response.setValorDescuentoGlobal(cotizacion.getValorDescuentoGlobal());
        response.setTipoDescuentoGlobal(cotizacion.getTipoDescuentoGlobal());
        response.setMontoDescuentoGlobalCalculado(cotizacion.getMontoDescuentoGlobalCalculado());

        response.setTotalNeto(cotizacion.getTotalNeto());
        response.setCostoGeneral(cotizacion.getCostoGeneral());
        response.setEstado(cotizacion.getEstado());

        response.setUsuarioId(cotizacion.getUsuario().getUsuarioId());
        if (cotizacion.getUsuario().getApellido() == null) {
            response.setUsuarioNombre(cotizacion.getUsuario().getNombre());
        } else {
            response.setUsuarioNombre(cotizacion.getUsuario().getNombre() + " " + cotizacion.getUsuario().getApellido());
        }

        if (cotizacion.getCliente() != null) {
            response.setClienteId(cotizacion.getCliente().getClienteId());

            if (cotizacion.getCliente().getApellido() == null) {
                response.setClienteNombre(cotizacion.getCliente().getNombre());
            } else {
                response.setClienteNombre(cotizacion.getCliente().getNombre() + " " + cotizacion.getCliente().getApellido());
            }
        }

        List<DetalleCotizacionResponse> detalles = cotizacion.getDetalles().stream()
                .map(this::toDetalleResponse).toList(); // Llamada helper
        response.setDetalles(detalles);

        return response;
    }

    // Helper privado para crear la respuesta de detalle de cotización
    private DetalleCotizacionResponse toDetalleResponse(DetalleCotizacion det) {
        DetalleCotizacionResponse d = new DetalleCotizacionResponse();
        d.setDetalleCotizacionId(det.getDetalleCotizacionId());
        d.setCodigoBarras(det.getCodigoBarras());
        d.setProductoId(det.getProducto().getProductoId());
        d.setProductoNombre(det.getProducto().getNombre());
        d.setCantidad(det.getCantidad());
        d.setPrecioUnitario(det.getPrecioUnitario());
        d.setValorDescuentoUnitario(det.getValorDescuentoUnitario());
        d.setTipoDescuentoUnitario(det.getTipoDescuentoUnitario());
        d.setSubtotal(det.getSubtotal());
        return d;
    }
}
