package com.decoaromas.decoaromaspos.dto.usuario;

import com.decoaromas.decoaromaspos.enums.Rol;
import lombok.Data;

@Data // De Lombok
public class UsuarioFilterDTO {
    // Filtros String parciales
    private String nombreCompletoParcial;
    private String correoParcial;
    private String usernameParcial;

    // Filtros exactos
    private Rol rol;
    private Boolean activo;
}