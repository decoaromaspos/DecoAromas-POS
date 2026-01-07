package com.decoaromas.decoaromaspos.dto.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarProductoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @Pattern(regexp = "^\\S*$", message = "El SKU no puede contener espacios en blanco")
    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotNull(message = "El precio detalle es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    private Double precioDetalle;

    private Double precioMayorista;

    @Positive(message = "El costo debe ser positivo")
    private Double costo;

    @Positive(message = "La id ser positiva")
    private Long familiaId;

    @Positive(message = "La id ser positiva")
    private Long aromaId;

}