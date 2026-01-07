package com.decoaromas.decoaromaspos.dto.cliente;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteResponse {
    private Long clienteId;
    private String rut;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String ciudad;
    private TipoCliente tipo;
    private Boolean activo;
}