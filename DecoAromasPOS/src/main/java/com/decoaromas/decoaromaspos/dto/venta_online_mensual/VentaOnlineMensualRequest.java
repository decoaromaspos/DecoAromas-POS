package com.decoaromas.decoaromaspos.dto.venta_online_mensual;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para registrar o actualizar el consolidado mensual de ventas online")
public class VentaOnlineMensualRequest {

    @Schema(description = "Año del registro (Igual o superior a 2015)", example = "2024", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El año es obligatorio")
    @Positive(message = "El año debe ser positivo")
    @Min(value = 2015, message = "El año no puede ser menor a 2015")
    private Integer anio;

    @Schema(description = "Mes del registro (1-12)", example = "5", minimum = "1", maximum = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El mes es obligatorio")
    @Positive(message = "El numero de mes debe ser positivo")
    @Max(value = 12, message = "El mes no puede ser mayor a 12")
    private Integer mes;

    @Schema(description = "Monto total de ventas al detalle", example = "150000.50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El total detalle es obligatorio")
    @Min(value = 0, message = "El total detalle no puede ser negativo")
    private Double totalDetalle;

    @Schema(description = "Monto total de ventas mayoristas", example = "500000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El total mayorista es obligatorio")
    @Min(value = 0, message = "El total mayorista no puede ser negativo")
    private Double totalMayorista;
}