package com.decoaromas.decoaromaspos.dto.usuario;

import com.decoaromas.decoaromaspos.enums.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRequest {

    @NotBlank(message = "Debe ingresar un nombre")
    private String nombre;
    private String apellido;

    @NotBlank(message = "Debe ingresar un correo")
    @Email(message = "Debe ingresar un correo válido")
    private String correo;

    @NotBlank(message = "Debe ingresar un nombre de usuario o apodo")
    private String username;

    @NotBlank(message = "Debe ingresar una contraseña")
    private String password;

    @NotNull(message = "Debe seleccionar un tipo de rol (ADMIN o VENDEDOR)")
    private Rol rol;


    // --- SETTERS PERSONALIZADOS ---
    // Limpian los datos ANTES de que se ejecuten las validaciones

    /** Limpia el nombre quitando espacios al inicio y al final. */
    public void setNombre(String nombre) {
        if (nombre != null) {
            this.nombre = nombre.trim();
        } else {
            this.nombre = null;
        }
    }

    /** Limpia el apellido. Si el resultado es una cadena vacía, se establece como null. */
    public void setApellido(String apellido) {
        if (apellido == null || apellido.trim().isEmpty()) {
            this.apellido = null;
        } else {
            this.apellido = apellido.trim();
        }
    }

    /** Limpia el correo. Campo obligatorio. */
    public void setCorreo(String correo) {
        if (correo != null) {
            this.correo = correo.trim();
        } else {
            this.correo = null;
        }
    }

    /** Limpia el username. Campo obligatorio. */
    public void setUsername(String username) {
        if (username != null) {
            this.username = username.trim();
        } else {
            this.username = null;
        }
    }

}
