package com.decoaromas.decoaromaspos.mapper;

import com.decoaromas.decoaromaspos.dto.caja.CajaResponse;
import com.decoaromas.decoaromaspos.model.Caja;
import com.decoaromas.decoaromaspos.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class CajaMapper {

    public CajaResponse toResponse(Caja caja) {
        Usuario usuario = caja.getUsuario();
        return CajaResponse.builder()
                .cajaId(caja.getCajaId())
                .fechaApertura(caja.getFechaApertura())
                .efectivoApertura(caja.getEfectivoApertura())
                .fechaCierre(caja.getFechaCierre())
                .efectivoCierre(caja.getEfectivoCierre())
                .mercadoPagoCierre(caja.getMercadoPagoCierre())
                .bciCierre(caja.getBciCierre())
                .botonDePagoCierre(caja.getBotonDePagoCierre())
                .transferenciaCierre(caja.getTransferenciaCierre())
                .postCierre(caja.getPostCierre())
                .estado(caja.getEstado())
                .diferenciaReal(caja.getDiferenciaReal())
                .usuarioId(usuario.getUsuarioId())
                .nombreUsuario(usuario.getNombre() + " " + usuario.getApellido())
                .username(usuario.getUsername())
                .build();
    }
}
