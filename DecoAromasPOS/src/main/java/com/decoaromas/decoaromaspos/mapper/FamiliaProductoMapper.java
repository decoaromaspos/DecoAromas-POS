package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.familia.FamiliaResponse;
import com.decoaromas.decoaromaspos.model.FamiliaProducto;
import org.springframework.stereotype.Component;

@Component
public class FamiliaProductoMapper {

    public FamiliaResponse toResponse(FamiliaProducto familia) {
        return FamiliaResponse.builder()
                .familiaId(familia.getFamiliaId())
                .nombre(familia.getNombre())
                .isDeleted(familia.getIsDeleted())
                .build();
    }
}
