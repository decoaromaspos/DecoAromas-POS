package com.decoaromas.decoaromaspos.dto.producto;

import jakarta.validation.constraints.*;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @Pattern(regexp = "^\\S*$", message = "El SKU no puede contener espacios en blanco.")
    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotNull(message = "El precio detalle es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    private Double precioDetalle;

    @NotNull(message = "El precio detalle es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    private Double precioMayorista;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    @Min(value = 0, message = "El costo debe ser positivo")
    private Double costo;

    @Positive(message = "La id ser positiva")
    private Long familiaId;

    @Positive(message = "La id ser positiva")
    private Long aromaId;

    @NotNull(message = "Debe asociar un usuario")
    @Positive(message = "La id ser positiva")
    private Long usuarioId;
}
