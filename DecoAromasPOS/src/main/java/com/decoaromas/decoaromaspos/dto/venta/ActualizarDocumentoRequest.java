package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActualizarDocumentoRequest {
    @NotNull(message = "El tipo de documento no puede ser nulo")
    private TipoDocumento tipoDocumento; // BOLETA o FACTURA

    @NotBlank(message = "El número de documento no puede estar vacío")
    @Size(max = 30)
    private String numeroDocumento;
}
