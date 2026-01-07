package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.producto.ProductoResponse;
import com.decoaromas.decoaromaspos.model.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoResponse toResponse(Producto producto) {
        // Comprueba si la familia existe. Si no, usa null.
        Long familiaId = (producto.getFamilia() != null) ? producto.getFamilia().getFamiliaId() : null;
        String familiaNombre = (producto.getFamilia() != null) ? producto.getFamilia().getNombre() : null;

        // Comprueba si el aroma existe. Si no, usa null.
        Long aromaId = (producto.getAroma() != null) ? producto.getAroma().getAromaId() : null;
        String aromaNombre = (producto.getAroma() != null) ? producto.getAroma().getNombre() : null;

        return ProductoResponse.builder()
                .productoId(producto.getProductoId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .sku(producto.getSku())
                .codigoBarras(producto.getCodigoBarras())
                .precioDetalle(producto.getPrecioDetalle())
                .precioMayorista(producto.getPrecioMayorista())
                .stock(producto.getStock())
                .costo(producto.getCosto())
                .familiaId(familiaId)
                .familiaNombre(familiaNombre)
                .aromaId(aromaId)
                .aromaNombre(aromaNombre)
                .activo(producto.getActivo())
                .build();
    }
}
