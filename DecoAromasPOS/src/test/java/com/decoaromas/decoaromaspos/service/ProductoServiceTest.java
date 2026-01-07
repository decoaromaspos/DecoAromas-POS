package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.other.request.ActivateIdRequest;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.producto.*;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.ProductoMapper;
import com.decoaromas.decoaromaspos.model.*;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private FamiliaProductoService familiaService;
    @Mock
    private AromaService aromaService;
    @Mock
    private GestorInventarioService gestorInventarioService;
    @Mock
    private BarcodeService barcodeService;
    @Mock
    private ProductoMapper productoMapper;
    @InjectMocks
    private ProductoService productoService;
    private Producto producto;
    private ProductoResponse productoResponse;
    private FamiliaProducto familia;
    private Aroma aroma;

    @BeforeEach
    void setUp() {
        familia = new FamiliaProducto();
        familia.setFamiliaId(16L);
        familia.setNombre("Velas");

        aroma = new Aroma();
        aroma.setAromaId(1L);
        aroma.setNombre("Lavanda");

        producto = Producto.builder()
                .productoId(1L)
                .nombre("Vela clásica")
                .sku("VELA01")
                .precioDetalle(1000.0)
                .precioMayorista(800.0)
                .stock(10)
                .activo(true)
                .familia(familia)
                .aroma(aroma)
                .build();

        productoResponse = new ProductoResponse();
        productoResponse.setProductoId(1L);
        productoResponse.setNombre("Vela clásica");
        productoResponse.setActivo(true);

    }

    @Test
    @DisplayName("Test para listar todos los productos")
    void listarProductos_deberiaRetornarListaDeProductoResponse() {
        when(productoRepository.findAll()).thenReturn(List.of(producto));
        when(productoMapper.toResponse(producto)).thenReturn(productoResponse);

        List<ProductoResponse> result = productoService.listarProductos();

        assertEquals(1, result.size());
        assertEquals("Vela clásica", result.get(0).getNombre());
        verify(productoRepository).findAll();
    }

    @Test
    @DisplayName("Test para obtener producto por ID")
    void obtenerProductoPorId_existente_deberiaRetornarResponse() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoMapper.toResponse(producto)).thenReturn(productoResponse);

        ProductoResponse result = productoService.obtenerProductoPorId(1L);

        assertNotNull(result);
        assertEquals("Vela clásica", result.getNombre());
    }

    @Test
    @DisplayName("Test para obtener producto por ID inexistente")
    void obtenerProductoPorId_inexistente_deberiaLanzarExcepcion() {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerProductoPorId(1L));
    }

    @Test
    @DisplayName("Test para crear un producto y retornarlo")
    void crearProducto_deberiaCrearYRetornarProductoResponse() {
        ProductoRequest request = ProductoRequest.builder()
                .nombre("Vela nueva")
                .descripcion("Oferta 2025")
                .sku("VELA02")
                .precioDetalle(2000.0)
                .precioMayorista(1500.0)
                .stock(5)
                .costo(1000.0)
                .familiaId(16L)
                .aromaId(1L)
                .usuarioId(10L)
                .build();

        when(familiaService.obtenerFamiliaRealPorId(16L)).thenReturn(familia);
        when(aromaService.obtenerAromaRealPorId(1L)).thenReturn(aroma);
        when(productoRepository.findBySkuIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);
        when(barcodeService.generarEAN13(1L)).thenReturn("1234567890123");
        when(productoMapper.toResponse(producto)).thenReturn(productoResponse);

        ProductoResponse result = productoService.crearProducto(request);

        assertNotNull(result);
        assertEquals("Vela clásica", result.getNombre());
        verify(gestorInventarioService).registrarStockInicial(any(Producto.class), anyInt(), eq(10L));
    }

    @Test
    @DisplayName("Test para actualizar un producto existente")
    void actualizarProducto_existente_deberiaActualizarYRetornarProductoResponse() {
        ActualizarProductoRequest request = ActualizarProductoRequest.builder()
                .nombre("Vela premium")
                .descripcion("Edición limitada")
                .sku("VELA02")
                .precioDetalle(2500.0)
                .precioMayorista(1800.0)
                .costo(1000.0)
                .familiaId(16L)
                .aromaId(1L)
                .build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(familiaService.obtenerFamiliaRealPorId(16L)).thenReturn(familia);
        when(aromaService.obtenerAromaRealPorId(1L)).thenReturn(aroma);
        when(productoRepository.findBySkuIgnoreCase("VELA02")).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);
        when(productoMapper.toResponse(producto)).thenReturn(productoResponse);

        ProductoResponse result = productoService.actualizarProductoNoStock(1L, request);

        assertNotNull(result);
        assertEquals("Vela clásica", result.getNombre());
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Test para eliminar un producto existente")
    void eliminarProducto_existente_deberiaEliminar() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        productoService.eliminarProducto(1L);

        verify(productoRepository).delete(producto);
    }

    @Test
    @DisplayName("Test para verificar la disponibilidad de un SKU debe lanzar disponible")
    void checkSkuAvailability_disponible_deberiaRetornarDisponible() {
        when(productoRepository.findBySkuIgnoreCase("VELA01")).thenReturn(Optional.empty());
        AvailabilityResponse response = productoService.checkSkuAvailability("VELA01");

        assertTrue(response.isAvailable());
        assertEquals("SKU disponible.", response.getMessage());
    }

    @Test
    @DisplayName("Test para verificar la disponibilidad de un SKU en caso de no disponible")
    void checkSkuAvailability_noDisponible_deberiaRetornarNoDisponible() {
        when(productoRepository.findBySkuIgnoreCase("VELA01")).thenReturn(Optional.of(producto));
        AvailabilityResponse response = productoService.checkSkuAvailability("VELA01");

        assertFalse(response.isAvailable());
        assertEquals("SKU 'VELA01' ya en uso. Ingrese otro.", response.getMessage());
    }

    @Test
    @DisplayName("Test para obtener el total de producto debe retornar la suma")
    void getStockTotalDeProductos_deberiaRetornarSuma() {
        when(productoRepository.sumStockActivosByStockGreaterThanZero()).thenReturn(Optional.of(100.0));
        Double total = productoService.getStockTotalDeProductos();
        assertEquals(100.0, total);
    }

    @Test
    @DisplayName("Test para verificar y buscar un producto con un SKU inexistente, debe lanzar un error")
    void obtenerProductoPorSku_noExistente_deberiaLanzarExcepcion() {
        when(productoRepository.findBySkuIgnoreCase("SKU999")).thenReturn(Optional.empty()); // se ve que el sku no tenga nada

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> productoService.obtenerProductoPorSku("SKU999"));

        assertEquals("No existe producto con sku SKU999", ex.getMessage()); // se lanza la excepcion
        verify(productoRepository, times(1)).findBySkuIgnoreCase("SKU999");
    }

    @Test
    @DisplayName("Test para obtener el codigo barra, si no existe un producto lanza error")
    void obtenerProductoPorCodigoBarras_noExistente_deberiaLanzarExcepcion() {
        when(productoRepository.findByCodigoBarras("9999999999999")).thenReturn(Optional.empty());
        // todos los codigos de barra tienen numeros aleatorios y nunca iguales, este ejemplo es solo demostrativo
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> productoService.obtenerProductoPorCodigoBarras("9999999999999")); // como se puede ver, no se encuentra

        assertEquals("No existe producto con código de barras 9999999999999", ex.getMessage()); // lanza el error correspondiente
        verify(productoRepository, times(1)).findByCodigoBarras("9999999999999");
    }

    @Test
    @DisplayName("Test para verificar la disponibilidad de un nombre de un producto")
    void checkNombreAvailability_disponible_deberiaRetornarDisponible() {
        // Arrange
        String nombre = "Mikado Frutos Rojos";
        when(productoRepository.findByNombreIgnoreCaseAndActivoTrue(nombre.trim()))
                .thenReturn(List.of()); // No hay productos activos con ese nombre

        // Act
        AvailabilityResponse response = productoService.checkNombreAvailability(nombre);

        // Assert
        assertTrue(response.isAvailable());
        assertEquals("Nombre disponible.", response.getMessage());
        verify(productoRepository, times(1))
                .findByNombreIgnoreCaseAndActivoTrue(nombre.trim());
    }

    @Test
    @DisplayName("Test para verificar la disponibilidad de un SKU de un producto")
    void checkSkuAvailability_noExistente_deberiaRetornarDisponible() {
        when(productoRepository.findBySkuIgnoreCase("DM002")).thenReturn(Optional.empty()); // Se ve esta vacio o no existe

        AvailabilityResponse resp = productoService.checkSkuAvailability("DM002"); // Ocupe otro para no ocupar siempre el mismo

        assertTrue(resp.isAvailable());
    }

    @Test
    @DisplayName("Test para obtener un producto por ID, si no lo encuentra lanza excepcion")
    void obtenerProductoPorId_noExistente_deberiaLanzarResourceNotFoundException() {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerProductoPorId(1L));
    }

    @Test
    @DisplayName("Test para obtener los productos activos, debe retornar una lista de ellos")
    void obtenerProductosActivos_deberiaRetornarListaDeProductoResponse() {
        List<Producto> productos = List.of(producto);

        when(productoRepository.findByActivoTrue()).thenReturn(productos);
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(productoResponse);

        List<ProductoResponse> result = productoService.obtenerProductosActivos();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Vela clásica", result.get(0).getNombre());
        verify(productoRepository, times(1)).findByActivoTrue();
    }

    @Test
    @DisplayName("Test para obtener los productos inactivos, debe retornar una lista de ellos")
    void obtenerProductosInactivos_deberiaRetornarListaDeProductoResponse() {
        producto.setActivo(false);
        List<Producto> productos = List.of(producto);

        when(productoRepository.findByActivoFalse()).thenReturn(productos);
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(productoResponse);

        List<ProductoResponse> result = productoService.obtenerProductosInactivos();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(productoRepository, times(1)).findByActivoFalse();
    }

    @Test
    @DisplayName("Test para buscar productos por nombre parcial, debe retornar coincidencias")
    void buscarProductoPorNombreParcial_deberiaRetornarCoincidencias() {
        String nombre = "Vela";
        List<Producto> productos = List.of(producto);

        when(productoRepository.findByNombreContainingIgnoreCase(nombre)).thenReturn(productos);
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(productoResponse);

        List<ProductoResponse> result = productoService.buscarProductoPorNombreParcial(nombre);

        assertEquals(1, result.size());
        assertEquals("Vela clásica", result.get(0).getNombre());
        verify(productoRepository).findByNombreContainingIgnoreCase(nombre);
    }

    @Test
    @DisplayName("Test buscar un producto por nombre parcial, debe retornar solo si hay coincidencias de activos")
    void buscarProductoActivoPorNombreParcial_deberiaRetornarSoloActivos() {
        String nombre = "Vela";
        producto.setActivo(true);
        List<Producto> productos = List.of(producto);

        when(productoRepository.findByNombreContainingIgnoreCaseAndActivo(nombre, true))
                .thenReturn(productos);
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(productoResponse);

        List<ProductoResponse> result = productoService.buscarProductoActivoPorNombreParcial(nombre);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getNombre().contains("Vela"));
        verify(productoRepository).findByNombreContainingIgnoreCaseAndActivo(nombre, true);
    }

    @Test
    @DisplayName("Test para buscar productos por nombre parcial, debe retornar coincidencia de inactivos")
    void buscarProductoInactivoPorNombreParcial_deberiaRetornarSoloInactivos() {
        String nombre = "Vela";
        producto.setActivo(false); // Producto inactivo

        ProductoResponse productoInactivoResponse = new ProductoResponse();
        productoInactivoResponse.setProductoId(1L);
        productoInactivoResponse.setNombre("Vela clásica");
        productoInactivoResponse.setActivo(false);

        List<Producto> productos = List.of(producto);

        when(productoRepository.findByNombreContainingIgnoreCaseAndActivo(nombre, false))
                .thenReturn(productos);
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(productoInactivoResponse);

        List<ProductoResponse> result = productoService.buscarProductoInactivoPorNombreParcial(nombre);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getActivo());
        assertTrue(result.get(0).getNombre().contains("Vela"));
        verify(productoRepository).findByNombreContainingIgnoreCaseAndActivo(nombre, false);
    }

    @Test
    @DisplayName("Test para obtener productos fuera de stock mediante filtros, paginacion")
    void getProductosFueraStockFiltrados_deberiaRetornarPaginaDeProductosFueraDeStockFiltrados() {
        int page = 0;
        int size = 1;
        String sortBy = "nombre";
        Long aromaId = 1L;
        Long familiaId = 2L;
        String nombre = "Vela";

        // Producto fuera de stock
        Producto productoFueraStock = new Producto();
        productoFueraStock.setProductoId(1L);
        productoFueraStock.setNombre("Vela sin stock");
        productoFueraStock.setStock(0);
        productoFueraStock.setActivo(true);

        ProductoResponse productoFueraStockResponse = new ProductoResponse();
        productoFueraStockResponse.setProductoId(1L);
        productoFueraStockResponse.setNombre("Vela sin stock");
        productoFueraStockResponse.setActivo(true);

        List<Producto> productos = List.of(productoFueraStock);
        Page<Producto> productoPage = new PageImpl<>(productos);

        // Mock de repository y mapper
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productoPage);
        when(productoMapper.toResponse(any(Producto.class)))
                .thenReturn(productoFueraStockResponse);

        // Ejecutar
        PaginacionResponse<ProductoResponse> resultado = productoService.getProductosFueraStockFiltrados(
                page, size, sortBy, aromaId, familiaId, nombre
        );

        // Verificaciones
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Vela sin stock", resultado.getContent().get(0).getNombre());
        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getTotalPages());
        assertEquals(page, resultado.getPageNumber());
        assertEquals(size, resultado.getPageSize());
        assertTrue(resultado.isFirst());
        assertTrue(resultado.isLast());

        verify(productoRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(productoMapper, times(1)).toResponse(any(Producto.class));
    }

    @Test
    @DisplayName("Test para obtener productos bajo stock mediante filtros, paginacion")
    void getProductosBajoStockFiltrados_deberiaRetornarPaginaDeProductosConStockBajoFiltrados() {
        int page = 0;
        int size = 1;
        String sortBy = "nombre";
        Long aromaId = 1L;
        Long familiaId = 2L;
        String nombre = "Vela";
        Integer umbralMaximo = 5;

        // Producto con stock bajo
        Producto productoBajoStock = new Producto();
        productoBajoStock.setProductoId(1L);
        productoBajoStock.setNombre("Vela de soja");
        productoBajoStock.setStock(3);
        productoBajoStock.setActivo(true);

        ProductoResponse productoBajoStockResponse = new ProductoResponse();
        productoBajoStockResponse.setProductoId(1L);
        productoBajoStockResponse.setNombre("Vela de soja");
        productoBajoStockResponse.setActivo(true);

        List<Producto> productos = List.of(productoBajoStock);
        Page<Producto> productoPage = new PageImpl<>(productos);

        // Mock de repository y mapper
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productoPage);
        when(productoMapper.toResponse(any(Producto.class)))
                .thenReturn(productoBajoStockResponse);

        // Ejecutar el método del servicio
        PaginacionResponse<ProductoResponse> resultado = productoService.getProductosBajoStockFiltrados(
                page, size, sortBy, aromaId, familiaId, nombre, umbralMaximo
        );

        // Verificaciones
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Vela de soja", resultado.getContent().get(0).getNombre());
        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getTotalPages());
        assertEquals(page, resultado.getPageNumber());
        assertEquals(size, resultado.getPageSize());
        assertTrue(resultado.isFirst());
        assertTrue(resultado.isLast());

        verify(productoRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(productoMapper, times(1)).toResponse(any(Producto.class));
    }

    @Test
    @DisplayName("Test para obtener productos filtrados, debe retornar la pagina de los productos filtrados")
    void getProductosFiltrados_deberiaRetornarPaginaDeProductosFiltrados() {
        int page = 0;
        int size = 1;
        String sortBy = "nombre";
        Long aromaId = 1L;
        Long familiaId = 2L;
        Boolean activo = true;
        String nombre = "Vela";
        String sku = "VELA01";
        String codigoBarras = "1234567890123";

        // Producto que cumple con los filtros
        Producto productoFiltrado = new Producto();
        productoFiltrado.setProductoId(1L);
        productoFiltrado.setNombre("Vela aromática");
        productoFiltrado.setSku("VELA01");
        productoFiltrado.setCodigoBarras("1234567890123");
        productoFiltrado.setActivo(true);

        ProductoResponse productoFiltradoResponse = new ProductoResponse();
        productoFiltradoResponse.setProductoId(1L);
        productoFiltradoResponse.setNombre("Vela aromática");
        productoFiltradoResponse.setSku("VELA01");
        productoFiltradoResponse.setCodigoBarras("1234567890123");
        productoFiltradoResponse.setActivo(true);

        List<Producto> productos = List.of(productoFiltrado);
        Page<Producto> productoPage = new PageImpl<>(productos);

        // Mock de repository y mapper
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productoPage);
        when(productoMapper.toResponse(any(Producto.class)))
                .thenReturn(productoFiltradoResponse);

        // Ejecutar método
        PaginacionResponse<ProductoResponse> resultado = productoService.getProductosFiltrados(
                page, size, sortBy, aromaId, familiaId, activo, nombre, sku, codigoBarras
        );

        // Verificaciones
        assertNotNull(resultado);
        assertEquals(1, resultado.getContent().size());
        assertEquals("Vela aromática", resultado.getContent().get(0).getNombre());
        assertEquals("VELA01", resultado.getContent().get(0).getSku());
        assertEquals("1234567890123", resultado.getContent().get(0).getCodigoBarras());
        assertTrue(resultado.getContent().get(0).getActivo());

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1, resultado.getTotalPages());
        assertEquals(page, resultado.getPageNumber());
        assertEquals(size, resultado.getPageSize());
        assertTrue(resultado.isFirst());
        assertTrue(resultado.isLast());

        // Verificar interacciones
        verify(productoRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(productoMapper, times(1)).toResponse(any(Producto.class));
    }

    @Test
    @DisplayName("Test para generar los codigos de barra faltantes, debe asignar y guardar")
    void generarCodigosBarrasFaltantes_deberiaAsignarCodigosYGuardarProductos() {
        // Arrange
        Producto productoSinCodigo = new Producto();
        productoSinCodigo.setProductoId(1L);
        productoSinCodigo.setNombre("Vela sin código");
        productoSinCodigo.setCodigoBarras(null);

        List<Producto> productosSinCodigo = List.of(productoSinCodigo);
        String codigoGenerado = "1234567890123";

        when(productoRepository.findByCodigoBarrasIsNull())
                .thenReturn(productosSinCodigo);
        when(barcodeService.generarEAN13(1L))
                .thenReturn(codigoGenerado);

        // Act
        productoService.generarCodigosBarrasFaltantes();

        // Assert
        assertEquals(codigoGenerado, productoSinCodigo.getCodigoBarras());
        verify(productoRepository, times(1)).findByCodigoBarrasIsNull();
        verify(barcodeService, times(1)).generarEAN13(1L);
        verify(productoRepository, times(1)).saveAll(productosSinCodigo);
    }

    @Test
    @DisplayName("Test para actualizar el stock absoluto de un producto")
    void actualizarStock_deberiaActualizarYRetornarResponse() {
        Long productoId = 1L;
        ActualizarStockRequest request = ActualizarStockRequest.builder()
                .nuevaCantidad(50)
                .usuarioId(10L)
                .build();

        when(gestorInventarioService.actualizarStockAbsoluto(productoId, 50, 10L))
                .thenReturn(producto);
        when(productoMapper.toResponse(producto)).thenReturn(productoResponse);

        ProductoResponse result = productoService.actualizarStock(productoId, request);

        assertNotNull(result);
        assertEquals("Vela clásica", result.getNombre());
        verify(gestorInventarioService, times(1))
                .actualizarStockAbsoluto(productoId, 50, 10L);
        verify(productoMapper, times(1)).toResponse(producto);
    }

    @Test
    @DisplayName("Test para cambiar el estado activo/inactivo de un producto")
    void cambiarEstadoActivo_deberiaActualizarEstadoYRetornarResponse() {
        ActivateIdRequest request = new ActivateIdRequest();
        request.setId(1L);
        request.setActivo(false);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);
        when(productoMapper.toResponse(producto)).thenReturn(productoResponse);

        ProductoResponse result = productoService.cambiarEstadoActivo(request);

        assertNotNull(result);
        verify(productoRepository, times(1)).save(producto);
        assertFalse(producto.getActivo());
    }

    @Test
    @DisplayName("Test para buscar producto seleccionado con nombre parcial, nombre válido retorna lista")
    void testBuscarProductoPorNombreParcialSelect_Valido() {
        // Datos del mock
        ProductoAutoCompleteSelectProjection p1 = mock(ProductoAutoCompleteSelectProjection.class);
        ProductoAutoCompleteSelectProjection p2 = mock(ProductoAutoCompleteSelectProjection.class);

        List<ProductoAutoCompleteSelectProjection> esperado = List.of(p1, p2);

        when(productoRepository.findByNombreContainingIgnoreCase(
                eq("vela"),
                any(Pageable.class)
        )).thenReturn(esperado);

        List<ProductoAutoCompleteSelectProjection> resultado =
                productoService.buscarProductoPorNombreParcialSelect("vela");
        assertEquals(2, resultado.size());
        verify(productoRepository, times(1))
                .findByNombreContainingIgnoreCase(eq("vela"), any(Pageable.class));
    }

    @Test
    @DisplayName("Test para buscar producto seleccionado por nombre parcial, nombre vacío retorna lista vacía")
    void testBuscarProductoPorNombreParcialSelect_Vacio() {

        List<ProductoAutoCompleteSelectProjection> resultado =
                productoService.buscarProductoPorNombreParcialSelect("   ");

        assertTrue(resultado.isEmpty());
        verify(productoRepository, never())
                .findByNombreContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Test para buscar producto seleccionado por nombre parcial, nombre nulo retorna lista vacía")
    void testBuscarProductoPorNombreParcialSelect_Nulo() {

        List<ProductoAutoCompleteSelectProjection> resultado =
                productoService.buscarProductoPorNombreParcialSelect(null);

        assertTrue(resultado.isEmpty());
        verify(productoRepository, never())
                .findByNombreContainingIgnoreCase(anyString(), any(Pageable.class));
    }

}
