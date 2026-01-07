package com.decoaromas.decoaromaspos.dto.venta;

import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Filtros para búsqueda avanzada de ventas")
public class VentaFilterDTO {

    @Schema(description = "Buscar por tipo de cliente (MAYORISTA, DETALLE)", examples = "MAYORISTA")
    private TipoCliente tipoCliente;

    @Schema(description = "Buscar por tipo de documento (BOLETA, FACTURA)", examples = "BOLETA")
    private TipoDocumento tipoDocumento;

    @Schema(description = "Buscar por medio de pago", examples = "EFECTIVO")
    private MedioPago medioPago;

    @Schema(description = "Buscar por total mínimo de venta", examples = "1000")
    private Double minTotalNeto;

    @Schema(description = "Buscar por total máximo de venta", examples = "2000")
    private Double maxTotalNeto;

    @Schema(description = "Buscar por ID de usuario", examples = "1")
    private Long usuarioId;

    @Schema(description = "Buscar por ID de cliente", examples = "2")
    private Long clienteId; // valor especial (ej.: 0 o -1) para indicar "sin cliente"

    @Schema(description = "Buscar por número de documento parcial", examples = "B123")
    private String numeroDocumentoParcial;

    @Schema(description = "Buscar por venta pendiente de asignación (Boolean)", examples = "true")
    private Boolean pendienteAsignacion;

    @Schema(description = "Buscar por ID de producto", examples = "2")
    private Long productoId;
}
