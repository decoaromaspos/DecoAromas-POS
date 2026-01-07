package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.venta_online_mensual.VentaOnlineMensualResponse;
import com.decoaromas.decoaromaspos.model.VentaOnlineMensual;
import org.springframework.stereotype.Component;

@Component
public class VentaOnlineMensualMapper {
    public VentaOnlineMensualResponse toResponse(VentaOnlineMensual ventaOnline){
        return VentaOnlineMensualResponse.builder()
                .id(ventaOnline.getVentaOnlineMensualId())
                .anio(ventaOnline.getAnio())
                .mes(ventaOnline.getMes())
                .totalDetalle(ventaOnline.getTotalDetalle())
                .totalMayorista(ventaOnline.getTotalMayorista())
                .build();
    }
}
