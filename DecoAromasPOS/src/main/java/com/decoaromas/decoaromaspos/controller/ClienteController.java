package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.cliente.ClienteFilterDTO;
import com.decoaromas.decoaromaspos.dto.cliente.ClienteRequest;
import com.decoaromas.decoaromaspos.dto.cliente.ClienteResponse;
import com.decoaromas.decoaromaspos.dto.other.request.ActivateIdRequest;
import com.decoaromas.decoaromaspos.dto.other.request.EmailRequest;
import com.decoaromas.decoaromaspos.dto.other.request.RutRequest;
import com.decoaromas.decoaromaspos.dto.other.response.*;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ExportException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.ClienteService;
import com.decoaromas.decoaromaspos.service.exports.ClienteExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Gestión de Clientes", description = "API para la administración de clientes (Mayoristas y Detalle).")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/clientes\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteExportService exportService;

    @Operation(summary = "Listar todos los clientes", description = "Obtiene una lista completa de clientes (activos e inactivos).")
    @ApiResponse(responseCode = "200", description = "Lista obtenida de clientes",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class))))
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ClienteResponse>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    @Operation(summary = "Listar clientes activos", description = "Obtiene una lista completa de clientes activos.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida de clientes activos",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class))))
    @GetMapping("/activos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ClienteResponse>> listarClientesActivos() {
        return ResponseEntity.ok(clienteService.listarClientesActivos());
    }

    @Operation(summary = "Listar clientes inactivos", description = "Obtiene una lista completa de clientes inactivos.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida de clientes inactivos",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class))))
    @GetMapping("/inactivos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ClienteResponse>> listarClientesInactivos() {
        return ResponseEntity.ok(clienteService.listarClientesInactivos());
    }

    @Operation(summary = "Obtener cliente por ID", description = "Devuelve el cliente correspondiente al ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Obtiene cliente según Id",
                    content =  @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Id de cliente inválido (debe ser número)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No existe cliente con id 123.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 404}")))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ClienteResponse> obtenerClientePorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerClientePorId(id));
    }

    @Operation(summary = "Obtener cliente por RUT", description = "Devuelve el cliente correspondiente al RUT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Obtiene cliente según RUT",
                    content =  @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "RUT inválido (debe ser string con formato)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No existe cliente con rut 1111111-1.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 404}")))
    })
    @GetMapping("/rut")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ClienteResponse> obtenerClientePorRut(@Valid @RequestBody RutRequest rut) {
        return ResponseEntity.ok(clienteService.obtenerClientePorRut(rut));
    }

    @Operation(summary = "Obtener cliente por correo", description = "Devuelve el cliente correspondiente al correo electrónico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Obtiene cliente según correo",
                    content =  @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Correo inválido (debe ser string con formato)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No existe cliente con correo correo@mail.com\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 404}")))
    })
    @GetMapping("/correo")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ClienteResponse> obtenerClientePorCorreo(@Valid @RequestBody EmailRequest correoRequest) {
        return ResponseEntity.ok(clienteService.obtenerClientePorCorreo(correoRequest));
    }

    @Operation(summary = "Listar clientes paginados y filtrados", description = "Devuelve una lista paginada de clientes aplicando filtros dinámicos.")
    @ApiResponse(responseCode = "200", description = "Paginación de clientes obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @PostMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<ClienteResponse>> getClientesFiltrados(
            @Parameter(description = "Número de página (0..N)")  @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "clienteId") String sortBy,
            @Parameter(description = "Body con elementos de filtrado de cliente") @Valid @RequestBody ClienteFilterDTO dto) {
        return ResponseEntity.ok(clienteService.getClientesFiltradosPaginados(page, size, sortBy, dto));
    }

    @Operation(summary = "Buscar clientes por nombre parcial", description = "Devuelve clientes que coincidan parcialmente con el nombre.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida de clientes según nombre",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Nombre parcial inválido (debe ser string)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),
    })
    @GetMapping("/buscar/nombre/{nombre}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ClienteResponse>> buscarClientePorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(clienteService.buscarClientesPorNombreParcial(nombre));
    }

    @Operation(summary = "Buscar clientes por nombre y apellido parcial", description = "Devuelve clientes que coincidan parcialmente con el nombre y apellido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida de clientes según nombre y apellido",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Nombre o apellido parcial inválido (deben ser string)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),
    })
    @GetMapping("/buscar/nombre/{nombre}/apellido/{apellido}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ClienteResponse>> buscarClientePorNombreYApellido(@PathVariable String nombre, @PathVariable String apellido) {
        return ResponseEntity.ok(clienteService.buscarClientesPorNombreYApellidoParcial(nombre, apellido));
    }

    @Operation(summary = "Verificar disponibilidad de RUT", description = "Comprueba si un RUT está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "RUT disponible", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"RUT disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "RUT ya existe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"RUT ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-rut")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkRutAvailability(@RequestParam String rut) {
        AvailabilityResponse response = clienteService.checkRutAvailability(rut);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
        }

        return new ResponseEntity<>(response, HttpStatus.OK); // 200 OK
    }

    @Operation(summary = "Verificar disponibilidad de correo", description = "Comprueba si un correo electrónico está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Correo disponible", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"Correo disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "Correo ya existe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"Correo ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-correo")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkCorreoAvailability(@RequestParam String correo) {
        AvailabilityResponse response = clienteService.checkCorreoAvailability(correo);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
        }

        return new ResponseEntity<>(response, HttpStatus.OK); // 200 OK
    }

    @Operation(summary = "Verificar disponibilidad de teléfono", description = "Comprueba si un teléfono está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Teléfono disponible", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"Teléfono disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "Teléfono ya existe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"Teléfono ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-telefono")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkTelefonoAvailability(@RequestParam String telefono) {
        AvailabilityResponse response = clienteService.checkTelefonoAvailability(telefono);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
        }

        return new ResponseEntity<>(response, HttpStatus.OK); // 200 OK
    }

    @Operation(summary = "Crear cliente", description = "Crea un nuevo cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado correctamente",
                    content =  @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación de formato de datos fallida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IllegalArgumentException.class))),
            @ApiResponse(responseCode = "409", description = "Datos ya existentes (teléfono, rut, correo)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExistsRegisterException.class),
                            examples = @ExampleObject(value = "{\"error\": \"El RUT 1111111-1 ya está registrado.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 409}"))),
            @ApiResponse(responseCode = "403", description = "Permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(summary = "Solo permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                                value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}")))
    })
    @PostMapping
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ClienteResponse> crearCliente(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.crearCliente(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado correctamente",
                    content =  @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación de formato de datos fallida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IllegalArgumentException.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado según Id",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No existe cliente con id 123.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 404}"))),
            @ApiResponse(responseCode = "409", description = "Datos ya existentes (teléfono, rut, correo)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExistsRegisterException.class),
                    examples = @ExampleObject(value = "{\"error\": \"El RUT 1111111-1 ya está registrado.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 409}"))),
            @ApiResponse(responseCode = "403", description = "Permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                    examples = @ExampleObject(summary = "Solo permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                            value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                    )))
    })
    @PutMapping("/update/{id}")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ClienteResponse> actualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @Operation(summary = "Cambiar estado activo", description = "Cambia el estado activo (true/false) de un cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado de cliente actualizado correctamente",
                    content =  @Content(mediaType = "application/json", schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación de formato de datos fallida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IllegalArgumentException.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado según Id",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(value = "{\"error\": \"No existe cliente con id 123.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 404}"))),
            @ApiResponse(responseCode = "403", description = "Permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(summary = "Solo permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )))
    })
    @PutMapping("/cambiar/estado")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ClienteResponse> cambiarEstadoActivo(@Valid @RequestBody ActivateIdRequest request) {
        return ResponseEntity.ok(clienteService.cambiarEstadoActivo(request.getId(), request.getActivo()));
    }

    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente por ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "403", description = "Requiere rol SUPER_ADMIN exclusivo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(summary = "Admin normal rechazado", value = "{\"error\": \"Acceso denegado\", \"status\": 403}")))
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener cantidad de clientes activos", description = "Devuelve la cantidad de clientes activos. 0 si no hay")
    @ApiResponse(responseCode = "200", description = "Ganancia del día obtenida correctamente.", content = @Content(examples = @ExampleObject(value = "{\"cantidadClientesActivos\": 150.0}")))
    @GetMapping("/activos/cantidad")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<Map<String, Double>> obtenerGananciasDelDiaActual() {
        return ResponseEntity.ok(clienteService.getCantidadClientesActivos());
    }

    @Operation(summary = "Exportar clientes a CSV", description = "Genera y descarga un archivo .csv con todos los clientes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo generado exitosamente", content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "500", description = "Error interno al generar el archivo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExportException.class),
                            examples = @ExampleObject(summary = "Error al exportar CSV", value = "{\"error\": \"Error al generar CSV de clientes.\", \"status\": 500}")))
    })
    @GetMapping("/exportar-csv")
    @PreAuthorize(IS_AUTHENTICATED)
    public void exportarCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Content-Disposition", "attachment; filename=\"clientes.csv\"");

        exportService.escribirClientesACsv(response.getWriter());
    }

}
