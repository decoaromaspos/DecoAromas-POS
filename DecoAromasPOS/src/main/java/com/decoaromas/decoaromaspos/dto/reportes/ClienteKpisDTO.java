package com.decoaromas.decoaromaspos.dto.reportes;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClienteKpisDTO {
    private long totalClientesActivos;
    private long totalClientesMayoristas;
    private long totalClientesDetalle;
    // Se podrían agregar más: (ej: tasa de retención, % de clientes mayoristas)
}