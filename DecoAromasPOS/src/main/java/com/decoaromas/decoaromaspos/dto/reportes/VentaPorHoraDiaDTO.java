package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaPorHoraDiaDTO {
    // 1=Domingo, 2=Lunes, ..., 7=Sábado (Estándar SQL DAYOFWEEK)
    private Integer diaSemana;
    private Integer hora;
    private Double total;
}