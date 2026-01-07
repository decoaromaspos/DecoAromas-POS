package com.decoaromas.decoaromaspos.dto.cliente;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos para crear o actualizar un cliente")
public class ClienteRequest {

    @Schema(description = "RUT chileno (sin puntos, con guion y dígito verificador)", example = "12345678-K", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Debe ingresar un RUT")
    @Size(min = 9, max = 10, message = "El RUT debe tener entre 9 y 10 caracteres")
    @Pattern(
            regexp = "^\\d{7,8}-[\\dK]$", // Regex mejorada: 7-8 dígitos, guion, y 0-9 o K mayúscula
            message = "El formato del RUT no es válido (ej: 12345678-K)")
    private String rut;

    @Schema(description = "Nombre del cliente", example = "Juan", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Debe ingresar un nombre")
    private String nombre;

    @Schema(description = "Apellido del cliente", example = "Pérez")
    private String apellido;

    @Schema(description = "Correo electrónico único", example = "juan.perez@email.com")
    @Email(message = "Debe ingresar un correo válido con @, ej: usuario@dominio.cl")
    private String correo;

    @Schema(description = "Teléfono móvil (formato internacional +569)", example = "+56987654321")
    @Pattern(
            regexp = "^\\+569\\d{8}$",
            message = "El teléfono debe tener el formato internacional para Chile, ej: +56987654321"
    )
    private String telefono;

    @Schema(description = "Ciudad de residencia", example = "Santiago")
    private String ciudad;

    // Lombok generará el setTipo() estándar, no necesitamos sobreescribirlo.
    // Necesitamos añadir este setter para que Lombok @AllArgsConstructor y @Builder funcionen
    // correctamente con los setters personalizados.
    @Schema(description = "Tipo de cliente (MAYORISTA / DETALLE)", example = "DETALLE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Debe seleccionar un tipo de cliente (MAYORISTA o DETALLE)")
    private TipoCliente tipo;



    // --- SETTERS PERSONALIZADOS ---
    // Limpian los datos ANTES de que se ejecuten las validaciones (@Pattern, @Email, etc.)

    /**
     * Normaliza el RUT:
     * 1. Elimina espacios al inicio y al final.
     * 2. Quita los puntos (ej.: "12.345.678-k").
     * 3. Convierte a mayúsculas (para la 'K').
     */
    public void setRut(String rut) {
        if (rut != null) {
            this.rut = rut.trim().replace(".", "").toUpperCase();
        } else {
            this.rut = null;
        }
    }

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

    /** Limpia el correo. Si el resultado es una cadena vacía, se establece como null. */
    public void setCorreo(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            this.correo = null;
        } else {
            this.correo = correo.trim();
        }
    }

    /** Limpia el teléfono. Si el resultado es una cadena vacía, se establece como null. */
    public void setTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            this.telefono = null;
        } else {
            this.telefono = telefono.trim();
        }
    }

    /** Limpia la ciudad. Si el resultado es una cadena vacía, se establece como null.*/
    public void setCiudad(String ciudad) {
        if (ciudad == null || ciudad.trim().isEmpty()) {
            this.ciudad = null;
        } else {
            this.ciudad = ciudad.trim();
        }
    }

}
