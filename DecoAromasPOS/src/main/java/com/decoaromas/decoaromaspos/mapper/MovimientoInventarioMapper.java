package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoInventarioResponse;
import com.decoaromas.decoaromaspos.model.MovimientoInventario;
import org.springframework.stereotype.Component;

@Component
public class MovimientoInventarioMapper {

    public MovimientoInventarioResponse toResponse(MovimientoInventario mov) {
        return MovimientoInventarioResponse.builder()
                .movimientoId(mov.getMovimientoId())
                .fecha(mov.getFecha())
                .tipo(mov.getTipo())
                .motivo(mov.getMotivo())
                .cantidad(mov.getCantidad())
                .productoId(mov.getProducto().getProductoId())
                .productoNombre(mov.getProducto().getNombre())
                .usuarioId(mov.getUsuario().getUsuarioId())
                .username(mov.getUsuario().getUsername())
                .nombreCompleto(mov.getUsuario().getNombre() + " " + mov.getUsuario().getApellido())
                .build();
    }
}
