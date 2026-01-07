package com.decoaromas.decoaromaspos.dto.movimiento_inventario;

import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
@Schema(description = "Detalle de un movimiento de stock en el inventario.")
public class MovimientoInventarioResponse {

    @Schema(description = "ID único del movimiento", example = "5001")
    private Long movimientoId;

    @Schema(description = "Fecha y hora en que ocurrió el movimiento (UTC/Zoned)", example = "2025-12-10T15:30:00-03:00")
    private ZonedDateTime fecha;

    @Schema(description = "Tipo de movimiento: ENTRADA (Aumenta stock) o SALIDA (Disminuye stock)", example = "SALIDA")
    private TipoMovimiento tipo;

    @Schema(description = "Motivo del cambio de stock (VENTA, CORRECCION, AJUSTE, etc.)", example = "VENTA")
    private MotivoMovimiento motivo;

    @Schema(description = "Cantidad de unidades afectadas (siempre positivo, el 'tipo' indica si suma o resta)", example = "2")
    private Integer cantidad;

    @Schema(description = "ID del producto afectado", example = "10")
    private Long productoId;

    @Schema(description = "Nombre del producto afectado", example = "Perfume Lavanda 100ml")
    private String productoNombre;

    @Schema(description = "ID del usuario que registró el movimiento", example = "3")
    private Long usuarioId;

    @Schema(description = "Username del usuario", example = "jdoe")
    private String username;

    @Schema(description = "Nombre completo del usuario", example = "Jane Doe")
    private String nombreCompleto;
}
