package com.decoaromas.decoaromaspos.dto.usuario;

import com.decoaromas.decoaromaspos.enums.Rol;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsuarioUpdateRol {
    @NotNull(message = "Debe ingresar un id de usuario")
    private Long usuarioId;
    @NotNull(message = "Debe seleccionar un tipo de rol (ADMIN o VENDEDOR)")
    private Rol rol;
}
