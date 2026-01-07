package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.cliente.ClienteResponse;
import com.decoaromas.decoaromaspos.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {
    public ClienteResponse toResponse(Cliente cliente) {
        return ClienteResponse.builder()
                .clienteId(cliente.getClienteId())
                .nombre(cliente.getNombre())
                .apellido(cliente.getApellido())
                .rut(cliente.getRut())
                .correo(cliente.getCorreo())
                .telefono(cliente.getTelefono())
                .tipo(cliente.getTipo())
                .ciudad(cliente.getCiudad())
                .activo(cliente.getActivo())
                .build();
    }
}
