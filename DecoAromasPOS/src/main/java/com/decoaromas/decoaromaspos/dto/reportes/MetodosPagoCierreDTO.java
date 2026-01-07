package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetodosPagoCierreDTO {
    private Double efectivoCierre;
    private Double mercadoPagoCierre;
    private Double bciCierre;
    private Double botonDePagoCierre;
    private Double transferenciaCierre;
    private Double postCierre;
}