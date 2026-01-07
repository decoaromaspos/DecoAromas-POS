package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.usuario.UsuarioResponse;
import com.decoaromas.decoaromaspos.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {
    public UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .usuarioId(usuario.getUsuarioId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .correo(usuario.getCorreo())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .nombreCompleto(usuario.getNombre() + " "  + usuario.getApellido())
                .build();
    }
}
