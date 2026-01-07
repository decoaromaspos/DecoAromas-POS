package com.decoaromas.decoaromaspos.dto.familia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaResponse {
    private Long familiaId;
    private String nombre;
    private Boolean isDeleted;
}
