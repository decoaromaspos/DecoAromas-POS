package com.decoaromas.decoaromaspos.dto.movimiento_inventario;

import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Filtros para búsqueda avanzada de movimientos")
public class MovimientoFilterDTO {
    @Schema(description = "Buscar por tipo de movimiento (ENTRADA o SALIDA)", examples = "SALIDA")
    private TipoMovimiento tipo;

    @Schema(description = "Buscar por motivo de movimiento (VENTA, AJUSTE, TRASPASO, etc.)", examples = "PRODUCCION")
    private MotivoMovimiento motivo;

    @Schema(description = "Buscar por ID de usuario", examples = "1")
    private Long usuarioId;

    @Schema(description = "Buscar por ID de producto", examples = "23")
    private Long productoId;
}
