package com.decoaromas.decoaromaspos.dto.reportes;

import java.time.ZonedDateTime;

public interface DescuadreCajaDTO {
    Long getCajaId();
    String getUsuario();
    ZonedDateTime getFechaCierre();
    Double getDiferencia();
}