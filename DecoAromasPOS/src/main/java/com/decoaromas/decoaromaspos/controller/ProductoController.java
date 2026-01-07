package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.request.ActivateIdRequest;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.dto.producto.*;
import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ExportException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.ProductoService;
import com.decoaromas.decoaromaspos.service.exports.ProductoExportService;
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
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Gestión de Productos", description = "API para crear, leer, actualizar, eliminar y gestionar stock de productos.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(
                                name = "Ejemplo 401",
                                summary = "Token inválido o faltante",
                                value = "{\"path\": \"/api/productos\", \"error\": \"No autorizado\", \"message\": \"Se requiere autenticación.\", \"status\": 401}"
                        )
                )
        )
})
public class ProductoController {

    private final ProductoService productoService;
    private final ProductoExportService exportService;

    // Lectura Básica elementos

    @Operation(summary = "Listar todos los productos", description = "Obtiene una lista completa de productos (activos e inactivos).")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoResponse.class))))
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductoResponse>> listarProductos(){
        return ResponseEntity.ok(productoService.listarProductos());
    }

    @Operation(summary = "Listar productos activos", description = "Devuelve solo los productos que están activos.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoResponse.class))))
    @GetMapping("/activos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductoResponse>> listarProductosActivos(){
        return ResponseEntity.ok(productoService.obtenerProductosActivos());
    }

    @Operation(summary = "Listar productos inactivos", description = "Devuelve solo los productos que están inactivos.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoResponse.class))))
    @GetMapping("/inactivos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductoResponse>> listarProductosInactivos(){
        return ResponseEntity.ok(productoService.obtenerProductosInactivos());
    }

    // Paginación y filtrado de elementos

    @Operation(summary = "Listar productos paginados y filtrados", description = "Devuelve una lista paginada de productos aplicando filtros dinámicos.")
    @ApiResponse(responseCode = "200", description = "Paginación obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<ProductoResponse>> listarProductosFiltradosPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "nombre") String sortBy,
            @Parameter(description = "Filtro de Id de aroma") @RequestParam(required = false) Long aromaId,
            @Parameter(description = "Filtro de Id de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro de Estado de producto: true (activo), false (inactivo)") @RequestParam(required = false) Boolean activo,
            @Parameter(description = "Filtro de nombre parcial") @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro de sku parcial") @RequestParam(required = false) String sku,
            @Parameter(description = "Filtro de código de barras parcial ") @RequestParam(required = false) String codigoBarras) {

        return ResponseEntity.ok(productoService.getProductosFiltrados(
                page, size, sortBy, aromaId, familiaId, activo, nombre, sku, codigoBarras));
    }

    // Búsqueda específica

    @Operation(summary = "Buscar productos por nombre parcial", description = "Devuelve productos que coincidan parcialmente con el nombre.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida de productos con nombre parcial ",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoResponse.class))))
    @GetMapping("/buscar/nombre/{nombre}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductoResponse>> buscarProductosPorNombreParcial(@PathVariable String nombre){
        return  ResponseEntity.ok(productoService.buscarProductoPorNombreParcial(nombre));
    }

    @Operation(summary = "Autocompletado de productos", description = "Búsqueda ligera para inputs de tipo select/autocomplete. Requiere mínimo 2 caracteres.")
    @ApiResponse(responseCode = "200", description = "Lista ligera de productos",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoAutoCompleteSelectProjection.class))))
    @GetMapping("/buscar/nombre/{nombre}/autocomplete")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductoAutoCompleteSelectProjection>> buscarProductosPorNombreParcialSelect(@PathVariable String nombre){
        if (nombre.length() < 2){
            // Retornar vacío si escriben menos de 2 letras para no saturar la BD
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(productoService.buscarProductoPorNombreParcialSelect(nombre));
    }

    @Operation(summary = "Buscar productos activos por nombre parcial", description = "Devuelve productos activos que coincidan parcialmente con el nombre.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoResponse.class))))
    @GetMapping("/activos/buscar/nombre/{nombre}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductoResponse>> buscarProductosActivosPorNombreParcial(@PathVariable String nombre) {
        return ResponseEntity.ok(productoService.buscarProductoActivoPorNombreParcial(nombre));
    }

    @Operation(summary = "Buscar productos inactivos por nombre parcial", description = "Devuelve productos inactivos que coincidan parcialmente con el nombre.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductoResponse.class))))
    @GetMapping("/inactivos/buscar/nombre/{nombre}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<ProductoResponse>> buscarProductosInactivosPorNombreParcial(@PathVariable String nombre) {
        return ResponseEntity.ok(productoService.buscarProductoInactivoPorNombreParcial(nombre));
    }

    @Operation(summary = "Obtener producto por ID", description = "Devuelve el producto correspondiente al ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según id",
                            value = "{\"error\": \"No existe producto con id 232\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}")))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ProductoResponse> obtenerProductoPorId(@PathVariable Long id){
        return ResponseEntity.ok(productoService.obtenerProductoPorId(id));
    }

    @Operation(summary = "Obtener producto por SKU", description = "Devuelve el producto correspondiente al SKU.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado por SKU", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según SKU",
                            value = "{\"error\": \"No existe producto con SKU A22\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}")))
    })
    @GetMapping("/sku/{sku}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ProductoResponse> obtenerProductoPorSku(@PathVariable String sku){
        return ResponseEntity.ok(productoService.obtenerProductoPorSku(sku));
    }

    @Operation(summary = "Obtener producto por código de barras", description = "Devuelve el producto correspondiente al código de barras.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según código de barras",
                            value = "{\"error\": \"No existe producto con código de barras 10000023\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}")))
    })
    @GetMapping("/codigo-barras/{codigoBarras}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<ProductoResponse> obtenerProductoPorCodigoBarras(@PathVariable String codigoBarras){
        return ResponseEntity.ok(productoService.obtenerProductoPorCodigoBarras(codigoBarras));
    }

    // Creación y edición de elementos

    @Operation(summary = "Crear producto", description = "Crea un nuevo producto. Si tiene stock > 0, genera un movimiento de inventario inicial.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (ej. precio negativo, nombre vacío)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IllegalArgumentException.class),
                            examples = @ExampleObject(value = "{\"error\": \"El nombre no puede estar vacío.\", \"status\": 409}"))),
            @ApiResponse(responseCode = "409", description = "Conflicto: SKU o Nombre ya existen",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExistsRegisterException.class),
                            examples = @ExampleObject(value = "{\"error\": \"Ya existe un producto (activo o inactivo) con SKU PD100. Ingrese otro SKU\", \"status\": 409}"))),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario sin rol ADMIN ni SUPER_ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )))
    })
    @PostMapping
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest request) {
        return new ResponseEntity<>(productoService.crearProducto(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar producto", description = "Actualiza datos básicos. No actualiza el stock directamente (usar endpoints de stock).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "ID no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según ID",
                            value = "{\"error\": \"No existe producto con id 23\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "409", description = "Conflicto: Nombre o SKU duplicado en otro producto",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExistsRegisterException.class),
                            examples = @ExampleObject(value = "{\"error\": \"Ya existe un producto (activo o inactivo) con SKU PD100. Ingrese otro SKU\", \"status\": 409}"))),
            @ApiResponse(responseCode = "400", description = "Validación dato fallida",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IllegalArgumentException.class))),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario sin rol ADMIN ni SUPER_ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )))
    })
    @PutMapping("/update/{id}")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ProductoResponse> actualizarProducto(@PathVariable Long id, @Valid @RequestBody ActualizarProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizarProductoNoStock(id, request));
    }

    @Operation(summary = "Cambiar estado activo", description = "Activa o desactiva un producto (Soft Delete / Habilitación).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Actualización de estado de producto", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "ID no encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según ID",
                            value = "{\"error\": \"No existe producto con id 23\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "403", description = "Requiere rol ADMIN o SUPER_ADMIN",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo 403",
                                    summary = "Usuario sin rol ADMIN ni SUPER_ADMIN",
                                    value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                            )))
    })
    @PutMapping("/cambiar/estado")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ProductoResponse> cambiarEstadoActivo(@RequestBody ActivateIdRequest request) {
        return ResponseEntity.ok(productoService.cambiarEstadoActivo(request));
    }


    // Gestión de Stock (PATCH)

    @Operation(summary = "Corrección de Stock (Absoluta)", description = "Establece el stock a una cantidad específica (ej. Inventario físico).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock actualizado correctamente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado con ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según ID",
                            value = "{\"error\": \"No existe producto con id 23\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "403", description = "Permitido para VENDEDOR, ADMIN y SUPER_ADMIN", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Ejemplo 403",
                            summary = "Solo permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                            value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                    )))
    })
    @PatchMapping("/stock/{id}")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ProductoResponse> actualizarStockProducto(@PathVariable Long id, @Valid @RequestBody ActualizarStockRequest request) {
        return ResponseEntity.ok(productoService.actualizarStock(id, request));
    }

    @Operation(summary = "Ingresar stock", description = "Suma cantidad al stock actual (Entrada de mercancía).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingreso de stock correcto", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado con ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según ID",
                            value = "{\"error\": \"No existe producto con id 23\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "403", description = "Permitido para VENDEDOR, ADMIN y SUPER_ADMIN", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Ejemplo 403",
                            summary = "Solo permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                            value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                    )))
    })
    @PatchMapping("/ingresar-stock/{id}")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ProductoResponse> ingresarStockProducto(@PathVariable Long id, @Valid @RequestBody MovimientoStockRequest request) {
        request.setTipo(TipoMovimiento.ENTRADA);
        request.setMotivo(MotivoMovimiento.NUEVO_STOCK);
        return ResponseEntity.ok(productoService.registrarMovimientoStock(id, request));
    }

    @Operation(summary = "Retirar stock", description = "Resta cantidad al stock actual (Corrección).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingreso de stock correcto", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado con ID", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResourceNotFoundException.class),
                    examples = @ExampleObject(summary = "Producto no encontrado según ID",
                            value = "{\"error\": \"No existe producto con id 23\"," +
                                    "\"timestamp\": \"2025-12-09T18:47:24.763249103}\"," +
                                    "\"status\": 404}"))),
            @ApiResponse(responseCode = "403", description = "Permitido para VENDEDOR, ADMIN y SUPER_ADMIN", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                    examples = @ExampleObject(
                            name = "Ejemplo 403",
                            summary = "Solo permitido para VENDEDOR, ADMIN y SUPER_ADMIN",
                            value = "{\"error\": \"Acceso denegado. No tienes los permisos necesarios.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 403}"
                    ))),
            @ApiResponse(responseCode = "500", description = "No se permite la salida manual debido a stock insuficiente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusinessException.class),
                    examples = @ExampleObject(
                            name = "Ejemplo 500",
                            summary = "Stock insuficiente para salida.",
                            value = "{\"error\": \"Stock insuficiente para realizar la salida manual.\", \"timestamp\": \"2025-11-29T16:38:02\", \"status\": 500}"
                    )))
    })
    @PatchMapping("/retirar-stock/{id}")
    @PreAuthorize(IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<ProductoResponse> retirarStockProducto(@PathVariable Long id, @Valid @RequestBody MovimientoStockRequest request) {
        request.setMotivo(MotivoMovimiento.CORRECCION);
        return ResponseEntity.ok(productoService.registrarMovimientoStock(id, request));
    }

    @Operation(summary = "Eliminar producto", description = "Eliminación física. Solo Super Admin.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "403", description = "Requiere rol SUPER_ADMIN exclusivo",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(summary = "Admin normal rechazado", value = "{\"error\": \"Acceso denegado\", \"status\": 403}")))
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id){
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }

    // Verificaciones de disponibilidad

    @Operation(summary = "Verificar disponibilidad de SKU", description = "Comprueba si un SKU está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SKU disponible", content = @Content(schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"SKU disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "SKU ya existe", content = @Content(schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"SKU ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-sku")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkSkuAvailability(@RequestParam String sku) {
        AvailabilityResponse response = productoService.checkSkuAvailability(sku);

        // NO disponible, 409 Conflict
        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
        }
        // Disponible, 200 OK
        return new ResponseEntity<>(response, HttpStatus.OK); // 200 OK
    }

    @Operation(summary = "Verificar disponibilidad de nombre", description = "Comprueba si un nombre de producto está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre disponible", content = @Content(schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"Nombre disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "Nombre en uso", content = @Content(schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"Nombre ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-nombre")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<AvailabilityResponse> checkNombreAvailabilityProducto(@RequestParam String nombre) {
        AvailabilityResponse response = productoService.checkNombreAvailability(nombre);

        // NO disponible, retornamos 409 Conflict
        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
        // Disponible, retornamos 200 OK
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Paginación utilidades de stock y reportes

    @Operation(summary = "Listar productos bajo stock paginados y filtrados", description = "Devuelve una lista paginada de productos con stock bajo aplicando filtros dinámicos.")
    @ApiResponse(responseCode = "200", description = "Paginación de productos bajo stock obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/bajo-stock/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<ProductoResponse>> listarProductosBajoStockFiltradosPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "stock") String sortBy,
            @Parameter(description = "Filtro de Id de aroma") @RequestParam(required = false) Long aromaId,
            @Parameter(description = "Filtro de Id de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro de nombre parcial") @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro de máximo de stock") @RequestParam(required = false, defaultValue = "20") Integer umbralMaximo) {
        return ResponseEntity.ok(productoService.getProductosBajoStockFiltrados(
                page, size, sortBy, aromaId, familiaId, nombre, umbralMaximo));
    }

    @Operation(summary = "Listar productos fuera de stock paginados y filtrados", description = "Devuelve una lista paginada de productos sin stock aplicando filtros dinámicos.")
    @ApiResponse(responseCode = "200", description = "Paginación de productos fuera de stock obtenida correctamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginacionResponse.class)))
    @GetMapping("/fuera-stock/filtros/paginas")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<PaginacionResponse<ProductoResponse>> listarProductosFueraStockFiltradosPaginados(
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "stock") String sortBy,
            @Parameter(description = "Filtro de Id de aroma") @RequestParam(required = false) Long aromaId,
            @Parameter(description = "Filtro de Id de familia") @RequestParam(required = false) Long familiaId,
            @Parameter(description = "Filtro de nombre parcial") @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(productoService.getProductosFueraStockFiltrados(
                page, size, sortBy, aromaId, familiaId, nombre));
    }

    @Operation(summary = "Obtener stock general", description = "Devuelve el stock total de todos los productos.")
    @ApiResponse(responseCode = "200", description = "Total calculado",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"stockGeneral\": 1250.0}")))
    @GetMapping("/stock/general")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<Map<String, Double>> obtenerStockTotalDeProductos() {
        Double stockGeneral = productoService.getStockTotalDeProductos();
        Map<String, Double> response = Map.of("stockGeneral", stockGeneral);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Generar códigos de barras faltantes",
            description = "Genera códigos de barras para productos que no los tienen. Útil para productos ingresados mediante sql sin código de barras.")
    @ApiResponse(responseCode = "204", description = "Generación de códigos de barra completa")
    @PutMapping("/generar/codigo-barras")
    @PreAuthorize(IS_SUPER_ADMIN)
    public ResponseEntity<Void> generarCodigosBarrasFaltantes() {
        productoService.generarCodigosBarrasFaltantes();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Exportar productos a CSV", description = "Genera y descarga un archivo .csv con todos los productos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archivo generado exitosamente", content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "500", description = "Error interno al generar el archivo",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExportException.class),
                            examples = @ExampleObject(summary = "Error al exportar CSV", value = "{\"error\": \"Error al generar CSV de productos.\", \"status\": 500}")))
    })
    @GetMapping("/exportar-csv")
    public void exportarCsv(
            @RequestParam(required = false) Long aromaId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Boolean activo,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Content-Disposition", "attachment; filename=\"productos.csv\"");

        exportService.escribirProductosACsv(response.getWriter(), aromaId, familiaId, activo);
    }
}
