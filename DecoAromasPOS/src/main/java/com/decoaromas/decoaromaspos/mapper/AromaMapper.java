package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.aroma.AromaResponse;
import com.decoaromas.decoaromaspos.model.Aroma;
import org.springframework.stereotype.Component;

@Component
public class AromaMapper {

    public AromaResponse toResponse(Aroma aroma) {
        return AromaResponse.builder()
                .aromaId(aroma.getAromaId())
                .nombre(aroma.getNombre())
                .isDeleted(aroma.getIsDeleted())
                .build();
    }
}
