package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteInactivoDTO {
    private Long clienteId;
    private String nombreCompleto;
    private ZonedDateTime ultimaCompra;
    private long diasInactivo; // Calculado en el servicio

    public ClienteInactivoDTO(Long clienteId, String nombre, String apellido, ZonedDateTime ultimaCompra) {
        this.clienteId = clienteId;
        this.nombreCompleto = nombre + " " + (apellido != null ? apellido : "");
        this.ultimaCompra = ultimaCompra;
    }
}