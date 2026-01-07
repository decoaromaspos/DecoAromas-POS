package com.decoaromas.decoaromaspos.dto.other.response;

import com.decoaromas.decoaromaspos.dto.usuario.UsuarioResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Respuesta exitosa de autenticación")
public class AuthSuccessResponse {

    @Schema(description = "Datos del usuario autenticado")
    private UsuarioResponse usuario;

    @Schema(description = "Token JWT de acceso", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
}