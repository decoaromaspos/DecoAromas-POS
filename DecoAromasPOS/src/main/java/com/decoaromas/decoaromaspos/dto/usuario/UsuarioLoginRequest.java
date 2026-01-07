package com.decoaromas.decoaromaspos.dto.usuario;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsuarioLoginRequest {
    @Schema(description = "Nombre de usuario o correo", example = "bast1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Debe ingresar un username")
    private String username;

    @Schema(description = "Contraseña del usuario", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Debe ingresar una contraseña")
    private String password;

    /**
     * Setter personalizado para el campo 'username'.
     * Jackson usará este método automáticamente al deserializar el JSON del request.
     * Esto asegura que cualquier espacio en blanco al inicio o al final
     * sea eliminado antes de que llegue al controlador.
     *
     * @param username El valor de 'username' proveniente del JSON.
     */
    @JsonSetter("username")
    public void setUsername(String username) {
        if (username != null) {
            // Aplicamos trim() para eliminar espacios al inicio y al final
            this.username = username.trim();
        } else {
            this.username = null;
        }
    }
}

