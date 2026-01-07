package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.other.request.ActivateIdRequest;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.producto.*;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.ProductoMapper;
import com.decoaromas.decoaromaspos.model.Aroma;
import com.decoaromas.decoaromaspos.model.FamiliaProducto;
import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import com.decoaromas.decoaromaspos.utils.ProductoSpecification;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Servicio para la gestión de la entidad Producto.
 * Se encarga del CRUD y la lógica de negocio del producto,
 * delegando toda la gestión de stock e inventario a {@link GestorInventarioService}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductoService {

    // --- Repositorio Propio ---
    private final ProductoRepository productoRepository;
    // --- Servicios Delegados ---
    private final FamiliaProductoService familiaService;
    private final AromaService aromaService;
    private final GestorInventarioService gestorInventarioService;
    private final BarcodeService barcodeService;
    // --- Mappers ---
    private final ProductoMapper productoMapper;


    // --- CONSULTAS (Lectura) ---

    /**
     * Obtiene una lista de todos los productos.
     * @return Lista de ProductoResponse.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductos() {
        return mapToList(productoRepository.findAll());
    }

    /**
     * Obtiene una lista de todos los productos marcados como 'activos'.
     * @return Lista de ProductoResponse.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerProductosActivos() {
        return mapToList(productoRepository.findByActivoTrue());
    }

    /**
     * Obtiene una lista de todos los productos marcados como 'inactivos'.
     * @return Lista de ProductoResponse.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerProductosInactivos() {
        return mapToList(productoRepository.findByActivoFalse());
    }

    /**
     * Busca productos (activos e inactivos) cuyo nombre contenga el texto proveído.
     * @param nombre Texto a buscar en el nombre del producto.
     * @return Lista de ProductoResponse.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarProductoPorNombreParcial(String nombre) {
        return mapToList(productoRepository.findByNombreContainingIgnoreCase(nombre));
    }

    @Transactional(readOnly = true)
    public List<ProductoAutoCompleteSelectProjection> buscarProductoPorNombreParcialSelect(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return List.of();
        }

        // Limitamos estrictamente a 20 resultados. Orden por nombre para que sea predecible.
        Pageable limit = PageRequest.of(0, 20, Sort.by("nombre").ascending());

        return productoRepository.findByNombreContainingIgnoreCase(nombre, limit);
    }


    /**
     * Busca productos 'activos' cuyo nombre contenga el texto proveído.
     * @param nombre Texto a buscar en el nombre del producto.
     * @return Lista de ProductoResponse.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarProductoActivoPorNombreParcial(String nombre) {
        return mapToList(productoRepository.findByNombreContainingIgnoreCaseAndActivo(nombre, true));
    }

    /**
     * Busca productos 'inactivos' cuyo nombre contenga el texto proveído.
     * @param nombre Texto a buscar en el nombre del producto.
     * @return Lista de ProductoResponse.
     */
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarProductoInactivoPorNombreParcial(String nombre) {
        return mapToList(productoRepository.findByNombreContainingIgnoreCaseAndActivo(nombre, false));
    }

    /**
     * Obtiene un producto por su ID.
     * @param id El ID del producto.
     * @return ProductoResponse.
     * @throws ResourceNotFoundException si no se encuentra el producto.
     */
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(Long id) {
        return productoMapper.toResponse(obtenerProductoRealPorId(id));
    }

    /**
     * Obtiene un producto por su SKU (insensible a mayúsculas/minúsculas y espacios).
     * @param sku El SKU del producto.
     * @return ProductoResponse.
     * @throws ResourceNotFoundException si no se encuentra el producto.
     */
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorSku(String sku) {
        String cleanedSku = sku.replaceAll("\\s+", "").toUpperCase();
        return productoMapper.toResponse(productoRepository.findBySkuIgnoreCase(cleanedSku)
                .orElseThrow(() -> new ResourceNotFoundException("No existe producto con sku " + cleanedSku)));
    }

    /**
     * Obtiene un producto por su Código de Barras.
     * @param codigoBarras El código de barras del producto.
     * @return ProductoResponse.
     * @throws ResourceNotFoundException si no se encuentra el producto.
     */
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorCodigoBarras(String codigoBarras) {
        return productoMapper.toResponse(productoRepository.findByCodigoBarras(codigoBarras)
                .orElseThrow(() -> new ResourceNotFoundException("No existe producto con código de barras " + codigoBarras)));
    }


    // --- ESCRITURA (Creación y Actualización) ---

    /**
     * Crea un nuevo producto en el sistema.
     * Genera un código de barras y, si el stock inicial es mayor a 0,
     * delega la creación del movimiento de inventario inicial.
     * @param request DTO con la información del producto a crear.
     * @return ProductoResponse del producto recién creado.
     * @throws ExistsRegisterException si el SKU ya está en uso.
     */
    public ProductoResponse crearProducto(ProductoRequest request) {
        // Validación de Entidades Relacionadas
        FamiliaProducto familia = (request.getFamiliaId() != null)
                ? familiaService.obtenerFamiliaRealPorId(request.getFamiliaId())
                : null;
        Aroma aroma = (request.getAromaId() != null)
                ? aromaService.obtenerAromaRealPorId(request.getAromaId())
                : null;

        // --- Limpieza de campos de texto ---
        String cleanedSku = request.getSku().replaceAll("\\s+", "").toUpperCase();
        String cleanedNombre = request.getNombre().trim();
        String cleanedDescripcion = (request.getDescripcion() != null)
                ? request.getDescripcion().trim()
                : null;

        // --- Validaciones unicidad ---
        if (productoRepository.findBySkuIgnoreCase(cleanedSku).isPresent()) {
            throw new ExistsRegisterException("Ya existe un producto (activo o inactivo) con SKU " + cleanedSku + ". Ingrese otro SKU");
        }
        if (cleanedNombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        if (!productoRepository.findByNombreIgnoreCaseAndActivoTrue(cleanedNombre).isEmpty()) {
            throw new ExistsRegisterException("Ya existe un producto ACTIVO con el nombre: " + cleanedNombre);
        }

        Producto producto = Producto.builder()
                .nombre(cleanedNombre)
                .descripcion(cleanedDescripcion)
                .sku(cleanedSku)
                .precioMayorista(request.getPrecioMayorista())
                .precioDetalle(request.getPrecioDetalle())
                .stock(request.getStock())
                .costo(request.getCosto())
                .familia(familia)
                .aroma(aroma)
                .activo(true)
                .build();

        producto = productoRepository.save(producto); // Guardar una vez para obtener el ID

        // --- Generación Código de Barras EAN-13 (Post-guardado) ---
        String codigoBarras = barcodeService.generarEAN13(producto.getProductoId());
        producto.setCodigoBarras(codigoBarras);
        producto = productoRepository.save(producto); // Actualizar con el código


        // --- DELEGACIÓN DE INVENTARIO ---
        // Si hay stock inicial, se delega el registro del movimiento.
        if (producto.getStock() != null && producto.getStock() > 0) {
            // Se delega la lógica de creación de movimiento a GestorInventarioService
            gestorInventarioService.registrarStockInicial(
                    producto,
                    producto.getStock(),
                    request.getUsuarioId()
            );
        }

        return productoMapper.toResponse(producto);
    }


    /**
     * Actualiza la información de un producto (campos de texto, precios, familia).
     * Este método NO actualiza el stock. Para eso, usar los métodos de inventario.
     * @param id      ID del producto a actualizar.
     * @param request DTO con los nuevos datos.
     * @return ProductoResponse actualizado.
     */
    public ProductoResponse actualizarProductoNoStock(Long id, ActualizarProductoRequest request) {
        Producto existente = obtenerProductoRealPorId(id);

        // --- Validación de Entidades Relacionadas ---
        FamiliaProducto familia = (request.getFamiliaId() != null)
                ? familiaService.obtenerFamiliaRealPorId(request.getFamiliaId())
                : null;
        Aroma aroma = (request.getAromaId() != null)
                ? aromaService.obtenerAromaRealPorId(request.getAromaId())
                : null;

        // --- Limpieza de campos ---
        String cleanedSku = request.getSku().replaceAll("\\s+", "").toUpperCase();
        String cleanedNombre = request.getNombre().trim(); // <-- Limpieza
        String cleanedDescripcion = (request.getDescripcion() != null)
                ? request.getDescripcion().trim()
                : null;


        if (cleanedNombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        // --- Validación de SKU (solo si cambia) ---
        productoRepository.findBySkuIgnoreCase(cleanedSku).ifPresent(p -> {
            if (!p.getProductoId().equals(id)) {
                throw new ExistsRegisterException("El SKU " + cleanedSku + " ya está asignado a otro producto.");
            }
        });

        // --- Validación de nombre de Producto Activo ---
        boolean nombreEnUsoPorOtro = !productoRepository
                .findByNombreIgnoreCaseAndActivoTrueAndProductoIdNot(cleanedNombre, id)
                .isEmpty();
        if (nombreEnUsoPorOtro) {
            throw new ExistsRegisterException("El nombre '" + cleanedNombre + "' ya está en uso por OTRO producto ACTIVO.");
        }

        // --- Limpieza y Actualización de campos ---
        existente.setNombre(cleanedNombre);
        existente.setDescripcion(cleanedDescripcion);
        existente.setSku(cleanedSku);
        existente.setPrecioDetalle(request.getPrecioDetalle());
        existente.setPrecioMayorista(request.getPrecioMayorista());
        existente.setCosto(request.getCosto());
        existente.setFamilia(familia);
        existente.setAroma(aroma);

        return productoMapper.toResponse(productoRepository.save(existente));
    }


    /**
     * Cambia el estado (activo/inactivo) de un producto.
     *
     * @param request DTO con el ID del producto y el nuevo estado (true, false).
     * @return ProductoResponse actualizado.
     */
    public ProductoResponse cambiarEstadoActivo(ActivateIdRequest request) {
        Producto producto = obtenerProductoRealPorId(request.getId());
        producto.setActivo(request.getActivo());
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    /**
     * Elimina un producto de la base de datos (borrado físico).
     * Nota: Añadir validación de integridad referencial si es necesario.
     *
     * @param id El ID del producto a eliminar.
     */
    public void eliminarProducto(Long id) {
        // Faltan validaciones de integridad.
        // No debería poder eliminarse un producto nunca.
        Producto producto = obtenerProductoRealPorId(id);
        productoRepository.delete(producto);
    }

    // --- LÓGICA DE INVENTARIO (Delegada) ---
    // Toda la lógica de actualización de stock e inventario se mueve a GestorInventarioService
    // para cumplir con el Principio de Responsabilidad Única (SRP).

    /**
     * Actualiza el stock de un producto a una cantidad absoluta (ej.: conteo de inventario).
     * Delega a GestorInventarioService para calcular la diferencia y crear el movimiento de 'CORRECCION'.
     * @param id      ID del producto.
     * @param request DTO con la nueva cantidad total y el usuario que realiza el ajuste.
     * @return ProductoResponse actualizado.
     */
    public ProductoResponse actualizarStock(Long id, ActualizarStockRequest request) {
        // Toda la lógica se delega al gestor de inventario.
        Producto productoActualizado = gestorInventarioService.actualizarStockAbsoluto(
                id,
                request.getNuevaCantidad(),
                request.getUsuarioId()
        );
        return productoMapper.toResponse(productoActualizado);
    }

    /**
     * Registra un movimiento de stock manual (ENTRADA o SALIDA) para un producto.
     * Delega a GestorInventarioService para validar, actualizar el stock y crear el movimiento.
     *
     * @param id      ID del producto.
     * @param request DTO con el tipo, motivo, cantidad y usuario.
     * @return ProductoResponse actualizado.
     */
    public ProductoResponse registrarMovimientoStock(Long id, MovimientoStockRequest request) {
        // Toda la lógica se delega al gestor de inventario.
        Producto productoActualizado = gestorInventarioService.registrarMovimientoManual(
                id,
                request.getCantidad(),
                request.getTipo(),
                request.getMotivo(),
                request.getUsuarioId()
        );
        return productoMapper.toResponse(productoActualizado);
    }



    // --- VALIDACIONES DE DISPONIBILIDAD ---

    /**
     * Verifica la disponibilidad de un SKU (insensible a mayúsculas/minúsculas y espacios).
     * @param sku El SKU a verificar.
     * @return DTO AvailabilityResponse indicando si está disponible.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkSkuAvailability(String sku) {
        String cleanedSku = sku.replaceAll("\\s+", "").toUpperCase();
        boolean exists = productoRepository.findBySkuIgnoreCase(cleanedSku).isPresent();

        if (exists) {
            return new AvailabilityResponse(
                    false,
                    "SKU '" + cleanedSku + "' ya en uso. Ingrese otro."
            );
        } else {
            return new AvailabilityResponse(
                    true,
                    "SKU disponible."
            );
        }
    }

    /**
     * Verifica la disponibilidad de un nombre de producto (exacto, sensible a espacios, no a mayúsculas).
     * Solo comprueba contra productos 'activos'.
     *
     * @param nombre El nombre a verificar.
     * @return DTO AvailabilityResponse indicando si está disponible.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkNombreAvailability(String nombre) {
        String cleanedNombre = nombre.trim();

        boolean disponible = productoRepository.findByNombreIgnoreCaseAndActivoTrue(cleanedNombre).isEmpty();

        if (disponible) {
            return new AvailabilityResponse(true, "Nombre disponible.");
        } else {
            return new AvailabilityResponse(
                    false,
                    " El nombre exacto '" + nombre + "' está en uso por algún producto activo. Ingrese otro o cambie el nombre del otro producto.");
        }
    }



    // --- BÚSQUEDA DE PRODUCTOS CON PAGINACIÓN Y FILTROS ---

    /**
     * Obtiene productos paginados y filtrados dinámicamente usando Specification.
     * @param page         Número de página (base 0).
     * @param size         Tamaño de la página.
     * @param sortBy       Campo para ordenar.
     * @param aromaId      Filtro por ID de aroma (opcional).
     * @param familiaId    Filtro por ID de familia (opcional).
     * @param activo       Filtro por estado (true, false, o null para todos).
     * @param nombre       Filtro por nombre (búsqueda 'like').
     * @param sku          Filtro por SKU (búsqueda 'like').
     * @param codigoBarras Filtro por código de barras (búsqueda 'like').
     * @return PaginacionResponse con los resultados.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<ProductoResponse> getProductosFiltrados(
            int page, int size, String sortBy,
            Long aromaId, Long familiaId, Boolean activo,
            String nombre, String sku, String codigoBarras
    ) {
        String cleanedNombre = (nombre == null) ? null : nombre.trim();
        String cleanedSku = (sku == null) ? null : sku.replaceAll("\\s+", "");
        String cleanedCodigoBarras = (codigoBarras == null) ? null : codigoBarras.replaceAll("\\s+", "");
        Specification<Producto> filtros = ProductoSpecification.conFiltros(
                aromaId,
                familiaId,
                activo,
                cleanedNombre,
                cleanedSku,
                cleanedCodigoBarras
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Producto> productoPage = productoRepository.findAll(filtros, pageable);

        Page<ProductoResponse> responsePage = productoPage.map(productoMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }

    /**
     * Obtiene productos paginados y filtrados que están "Bajo Stock" (stock <= umbral).
     * @param page         Número de página.
     * @param size         Tamaño de la página.
     * @param sortBy       Campo para ordenar.
     * @param aromaId      Filtro por ID de aroma (opcional).
     * @param familiaId    Filtro por ID de familia (opcional).
     * @param nombre       Filtro por nombre (búsqueda 'like').
     * @param umbralMaximo El valor máximo de stock para ser considerado "bajo stock".
     * @return PaginacionResponse con los resultados.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<ProductoResponse> getProductosBajoStockFiltrados(
            int page, int size, String sortBy,
            Long aromaId, Long familiaId,
            String nombre, Integer umbralMaximo
    ) {
        String cleanedNombre = (nombre == null) ? null : nombre.trim();
        Specification<Producto> filtros = ProductoSpecification.bajoStockconFiltros(aromaId, familiaId, cleanedNombre, umbralMaximo);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Producto> productoPage = productoRepository.findAll(filtros, pageable);

        Page<ProductoResponse> responsePage = productoPage.map(productoMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }

    /**
     * Obtiene productos paginados y filtrados que están "Fuera de Stock" (stock = 0).
     * @param page      Número de página.
     * @param size      Tamaño de la página.
     * @param sortBy    Campo para ordenar.
     * @param aromaId   Filtro por ID de aroma (opcional).
     * @param familiaId Filtro por ID de familia (opcional).
     * @param nombre    Filtro por nombre (búsqueda 'like').
     * @return PaginacionResponse con los resultados.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<ProductoResponse> getProductosFueraStockFiltrados(
            int page, int size, String sortBy,
            Long aromaId, Long familiaId, String nombre
    ) {
        String cleanedNombre = (nombre == null) ? null : nombre.trim();
        Specification<Producto> filtros = ProductoSpecification.fueraStockconFiltros(aromaId, familiaId, cleanedNombre);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Producto> productoPage = productoRepository.findAll(filtros, pageable);

        Page<ProductoResponse> responsePage = productoPage.map(productoMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }

    /**
     * Calcula la suma total del stock de todos los productos activos
     * que tienen stock mayor a cero.
     * @return Un Double con la suma del stock.
     */
    @Transactional(readOnly = true)
    public Double getStockTotalDeProductos() {
        return productoRepository.sumStockActivosByStockGreaterThanZero().orElse(0.0);
    }




    // --- LÓGICA INTERNA / UTILIDADES ---

    /**
     * Método de utilidad (público) para obtener la *entidad* Producto por su ID.
     * Usado por otros servicios (como VentaService, GestorInventario) para obtener la entidad real y no el DTO.
     * @param id El ID del producto.
     * @return La entidad Producto.
     * @throws ResourceNotFoundException si no se encuentra el producto.
     */
    public Producto obtenerProductoRealPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe producto con id " + id));
    }

    /**
     * Genera códigos de barras EAN-13 para todos los productos que no tengan uno.
     * Útil para productos antiguos migrados sin código.
     */
    public void generarCodigosBarrasFaltantes() {
        List<Producto> sinCodigo = productoRepository.findByCodigoBarrasIsNull();

        for (Producto producto : sinCodigo) {
            String codigo = barcodeService.generarEAN13(producto.getProductoId());
            producto.setCodigoBarras(codigo);
        }

        productoRepository.saveAll(sinCodigo);
    }

    /**
     * Helper privado para reducir la duplicación de `stream().map().toList()`.
     * @param productos Lista de entidades Producto.
     * @return Lista de ProductoResponse.
     */
    private List<ProductoResponse> mapToList(List<Producto> productos) {
        return productos.stream()
                .map(productoMapper::toResponse)
                .toList();
    }
}
