package com.decoaromas.decoaromaspos.dto.venta;

import lombok.Data;

@Data
public class ActualizarClienteRequest {
    private Long clienteId; // null permite "des-asociar" un cliente si es necesario
}
