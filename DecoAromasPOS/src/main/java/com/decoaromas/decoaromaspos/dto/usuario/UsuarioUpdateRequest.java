package com.decoaromas.decoaromaspos.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioUpdateRequest {

    @NotBlank(message = "Debe ingresar un nombre")
    private String nombre;

    private String apellido;

    @NotBlank(message = "Debe ingresar un correo")
    @Email(message = "Debe ingresar un correo válido")
    private String correo;

    @NotBlank(message = "Debe ingresar un nombre de usuario o apodo")
    private String username;



    // --- SETTERS PERSONALIZADOS ---

    public void setNombre(String nombre) {
        if (nombre != null) {
            this.nombre = nombre.trim();
        } else {
            this.nombre = null;
        }
    }

    public void setApellido(String apellido) {
        if (apellido == null || apellido.trim().isEmpty()) {
            this.apellido = null;
        } else {
            this.apellido = apellido.trim();
        }
    }

    public void setCorreo(String correo) {
        if (correo != null) {
            this.correo = correo.trim();
        } else {
            this.correo = null;
        }
    }

    public void setUsername(String username) {
        if (username != null) {
            this.username = username.trim();
        } else {
            this.username = null;
        }
    }
}
