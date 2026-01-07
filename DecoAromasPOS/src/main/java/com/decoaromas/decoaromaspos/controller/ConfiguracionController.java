package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.request.ConfigValueRequest;
import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.service.ConfiguracionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
@Tag(name = "Gestión de Configuración", description = "API para obtener y actualizar variables globales del sistema (Metas, IPs, etc.).")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/configuracion\", \"error\": \"No autorizado\", \"message\": \"Se requiere autenticación.\", \"status\": 401}")
                )
        )
})
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;
    private static final String VALOR_STRING = "valor";

    @Operation(summary = "Obtener valor de configuración", description = "Recupera el valor de una clave específica (ej. 'META_MENSUAL', 'IP_IMPRESORA').")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Valor encontrado",
                    content = @Content(mediaType = "text/plain",
                            schema = @Schema(type = "string", example = "1500000"))),
            @ApiResponse(responseCode = "404", description = "Clave de configuración no encontrada",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{clave}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<String> getConfiguracion(
            @Parameter(description = "Clave de la configuración", example = "META_MENSUAL")
            @PathVariable String clave) {
        String valor = configuracionService.getConfiguracionAsString(clave, null);
        if (valor != null) {
            return ResponseEntity.ok(valor);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Actualizar meta mensual", description = "Actualiza la meta mensual de ventas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta actualizada correctamente"),

            @ApiResponse(responseCode = "400", description = "Formato de número inválido o valor vacío",
                    content = @Content(mediaType = "application/json")),

            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class)))
    })
    @PutMapping("/meta-mensual")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<Void> actualizarMeta(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Objeto con la clave 'valor' conteniendo el monto numérico.",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfigValueRequest.class),
                            examples = @ExampleObject(value = "{\"valor\": \"2500000\"}"))
            )
            @RequestBody Map<String, String> payload) {
        try {
            String valorLimpio = payload.get(VALOR_STRING) != null ? payload.get(VALOR_STRING).trim() : null;
            if (valorLimpio == null || valorLimpio.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            BigDecimal nuevaMeta = new BigDecimal(valorLimpio);
            configuracionService.guardarConfiguracion("META_MENSUAL", nuevaMeta.toPlainString());
            return ResponseEntity.ok().build();
        } catch (NumberFormatException | NullPointerException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Actualizar IP de impresora", description = "Actualiza la dirección IP de la impresora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "IP actualizada correctamente"),

            @ApiResponse(responseCode = "400", description = "Valor vacío o inválido",
                    content = @Content(mediaType = "application/json")),

            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class)))
    })
    @PutMapping("/ip-impresora")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<Void> actualizarIpImpresora(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Objeto con la clave 'valor' conteniendo la dirección IP.",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConfigValueRequest.class),
                            examples = @ExampleObject(value = "{\"valor\": \"192.168.1.200\"}"))
            )
            @RequestBody Map<String, String> payload) {

        String nuevaIp = payload.get(VALOR_STRING) != null ? payload.get(VALOR_STRING).trim() : null;

        if (nuevaIp == null || nuevaIp.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        configuracionService.guardarConfiguracion("IP_IMPRESORA", nuevaIp);
        return ResponseEntity.ok().build();
    }

}