package com.decoaromas.decoaromaspos.dto.aroma;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AromaResponse {
    private Long aromaId;
    private String nombre;
    private Boolean isDeleted;
}
