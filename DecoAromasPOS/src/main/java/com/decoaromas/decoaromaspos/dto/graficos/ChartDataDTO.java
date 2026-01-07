package com.decoaromas.decoaromaspos.dto.graficos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estructura estándar para gráficos de líneas, barras o dispersión")
public class ChartDataDTO {

    @Schema(description = "Lista de series de datos (eje Y)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SeriesItemDTO> series;

    @Schema(description = "Categorías o etiquetas (eje X)", example = "[\"Ene\", \"Feb\", \"Mar\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> categories; // Para el eje X
}