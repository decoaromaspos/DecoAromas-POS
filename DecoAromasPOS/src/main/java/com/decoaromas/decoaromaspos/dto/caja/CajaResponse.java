package com.decoaromas.decoaromaspos.dto.caja;

import com.decoaromas.decoaromaspos.enums.EstadoCaja;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
public class CajaResponse {

    private Long cajaId;
    private ZonedDateTime fechaApertura;
    private Double efectivoApertura;
    private ZonedDateTime fechaCierre;

    private Double efectivoCierre;
    private Double mercadoPagoCierre;
    private Double bciCierre;
    private Double botonDePagoCierre;
    private Double transferenciaCierre;
    private Double postCierre;

    private EstadoCaja estado;
    private Double diferenciaReal;
    private Long usuarioId;
    private String nombreUsuario;
    private String username;
}
