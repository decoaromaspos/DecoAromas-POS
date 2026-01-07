package com.decoaromas.decoaromaspos.dto.usuario;

import com.decoaromas.decoaromaspos.enums.Rol;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioResponse {
    private Long usuarioId;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String correo;
    private String username;
    private Rol rol;
    private Boolean activo;
}
