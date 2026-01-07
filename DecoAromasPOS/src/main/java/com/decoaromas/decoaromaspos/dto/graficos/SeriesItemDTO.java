package com.decoaromas.decoaromaspos.dto.graficos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Un conjunto de datos para una serie del gráfico")
public class SeriesItemDTO {
    @Schema(description = "Nombre de la serie", example = "Ventas 2024")
    private String name;

    @Schema(description = "Valores numéricos", example = "[15000.0, 23000.5, 12000.0]")
    private List<Double> data;
}