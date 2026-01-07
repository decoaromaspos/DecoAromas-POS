package com.decoaromas.decoaromaspos.dto.cliente;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Filtros para búsqueda avanzada de clientes")
public class ClienteFilterDTO {
    @Schema(description = "Buscar por coincidencia en Nombre + Apellido", example = "Juan")
    private String nombreCompletoParcial;

    @Schema(description = "Buscar por parte del RUT", example = "1234")
    private String rutParcial;

    @Schema(description = "Buscar por parte del correo", example = "@gmail.com")
    private String correoParcial;

    @Schema(description = "Buscar por parte del teléfono", example = "9876")
    private String telefonoParcial;

    @Schema(description = "Buscar por ciudad", example = "Valparaíso")
    private String ciudadParcial;

    @Schema(description = "Filtrar por tipo exacto", example = "MAYORISTA")
    private TipoCliente tipo;

    @Schema(description = "Estado (true=Activo, false=Inactivo, null=Todos)", example = "true")
    private Boolean activo;
}