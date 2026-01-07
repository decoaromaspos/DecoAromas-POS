package com.decoaromas.decoaromaspos.dto.graficos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estructura para gráficos circulares (Torta/Donut)")
public class PieChartDataDTO {
    @Schema(description = "Valores numéricos proporcionales", example = "[300.0, 150.0]")
    private List<Double> series;

    @Schema(description = "Etiquetas de las porciones", example = "[\"Efectivo\", \"Tarjeta\"]")
    private List<String> labels;
}