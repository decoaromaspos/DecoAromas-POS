package com.decoaromas.decoaromaspos.dto.reportes;

import com.decoaromas.decoaromaspos.enums.TipoCliente;

public interface VentaPorTipoClienteDTO {
    TipoCliente getTipoCliente();
    Double getTotal();
}