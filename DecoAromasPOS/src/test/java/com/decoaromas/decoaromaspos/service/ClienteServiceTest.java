package com.decoaromas.decoaromaspos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decoaromas.decoaromaspos.dto.cliente.ClienteFilterDTO;
import com.decoaromas.decoaromaspos.dto.cliente.ClienteRequest;
import com.decoaromas.decoaromaspos.dto.cliente.ClienteResponse;
import com.decoaromas.decoaromaspos.dto.other.request.EmailRequest;
import com.decoaromas.decoaromaspos.dto.other.request.RutRequest;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.ClienteMapper;
import com.decoaromas.decoaromaspos.model.Cliente;
import com.decoaromas.decoaromaspos.repository.ClienteRepository;
import com.decoaromas.decoaromaspos.utils.AvailabilityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AvailabilityChecker checker;
    @Mock
    private ClienteMapper clienteMapper;
    @InjectMocks
    private ClienteService clienteService;
    private Cliente cliente;
    private ClienteRequest clienteRequest;
    private ClienteResponse clienteResponse;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setClienteId(1L);
        cliente.setRut("12345678-9");
        cliente.setCorreo("test@example.com");
        cliente.setTelefono("123456789");
        cliente.setActivo(true);
        cliente.setNombre("Juan");
        cliente.setApellido("Perez");
        cliente.setCiudad("Santiago");
        // Creacion de cliente para otros test
        clienteRequest = new ClienteRequest();
        clienteRequest.setRut("12345678-9");
        clienteRequest.setCorreo("test@example.com");
        clienteRequest.setTelefono("123456789");
        clienteRequest.setNombre("Juan");
        clienteRequest.setApellido("Perez");
        clienteRequest.setCiudad("Santiago");
        // Creacion de cliente para otros test
        clienteResponse = new ClienteResponse();
        clienteResponse.setClienteId(1L);
        clienteResponse.setRut("12345678-9");
        clienteResponse.setCorreo("test@example.com");
        clienteResponse.setTelefono("123456789");
        clienteResponse.setActivo(true);
        clienteResponse.setNombre("Juan");
        clienteResponse.setApellido("Perez");
        clienteResponse.setCiudad("Santiago");
    }

    @Test
    @DisplayName("Test para listar clientes, devuelve lista")
    void listarClientes_deberiaRetornarListaDeClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);
        List<ClienteResponse> result = clienteService.listarClientes();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("12345678-9", result.get(0).getRut());
        verify(clienteRepository).findAll();
        verify(clienteMapper).toResponse(cliente);
    }

    @Test
    @DisplayName("Test para obtener clientes por ID")
    void obtenerClientePorId_clienteExiste_retornaClienteResponse() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);

        ClienteResponse result = clienteService.obtenerClientePorId(1L);

        assertNotNull(result);
        assertEquals("12345678-9", result.getRut());
        verify(clienteRepository).findById(1L);
    }

    @Test
    @DisplayName("Test para obtener clientes por ID, si no existe lanza excepcion")
    void obtenerClientePorId_clienteNoExiste_lanzaResourceNotFoundException() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.obtenerClientePorId(1L));

        assertEquals("No existe cliente con id 1", exception.getMessage());
        verify(clienteRepository).findById(1L);
    }

    @Test
    @DisplayName("Test para crear cliente existoso")
    void crearCliente_exitoso_retornaClienteResponse() {
        when(clienteRepository.existsByRutIgnoreCase("12345678-9")).thenReturn(false);
        when(clienteRepository.existsByCorreoIgnoreCase("test@example.com")).thenReturn(false);
        when(clienteRepository.existsByTelefono("123456789")).thenReturn(false);

        when(clienteRepository.save(any())).thenReturn(cliente);
        when(clienteMapper.toResponse(any())).thenReturn(clienteResponse);

        ClienteResponse result = clienteService.crearCliente(clienteRequest);

        assertNotNull(result);
        assertEquals("12345678-9", result.getRut());
        verify(clienteRepository).save(any());
    }

    @Test
    @DisplayName("Test para crear cliente con rut existente, lanza excepcion de registro en caso contrario")
    void crearCliente_rutExistente_lanzaExistsRegisterException() {
        when(clienteRepository.existsByRutIgnoreCase("12345678-9")).thenReturn(true);

        ExistsRegisterException ex = assertThrows(ExistsRegisterException.class,
                () -> clienteService.crearCliente(clienteRequest));

        assertEquals("El RUT 12345678-9 ya está registrado.", ex.getMessage());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test para actualizar cliente de manera exitosa")
    void actualizarCliente_exitoso_retornaClienteResponse() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        clienteRequest.setRut("NUEVO-RUT");
        clienteRequest.setCorreo("nuevo@example.com");
        clienteRequest.setTelefono("000000000");
        when(clienteRepository.existsByRutIgnoreCaseAndClienteIdNot(anyString(), eq(1L))).thenReturn(false);
        when(clienteRepository.existsByCorreoIgnoreCaseAndClienteIdNot(anyString(), eq(1L))).thenReturn(false);
        when(clienteRepository.existsByTelefonoAndClienteIdNot(anyString(), eq(1L))).thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(cliente);
        when(clienteMapper.toResponse(any())).thenReturn(clienteResponse);
        ClienteResponse result = clienteService.actualizarCliente(1L, clienteRequest);
        assertNotNull(result);
        assertEquals("12345678-9", result.getRut());
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("Test para actualizar un cliente, si este no existe, lanza excepcion")
    void actualizarCliente_noExiste_lanzaResourceNotFoundException() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.actualizarCliente(1L, clienteRequest));

        assertEquals("No existe cliente con id 1", ex.getMessage());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test para cambiar el estado activo de un cliente, debe cambiarlo")
    void cambiarEstadoActivo_deberiaCambiarEstado() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(cliente)).thenReturn(cliente);
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);

        ClienteResponse response = clienteService.cambiarEstadoActivo(1L, false);

        assertNotNull(response);
        verify(clienteRepository).save(cliente);
        assertFalse(cliente.getActivo());
    }

    @Test
    @DisplayName("Test para eliminar cliente")
    void eliminarCliente_existente_eliminaCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        clienteService.eliminarCliente(1L);

        verify(clienteRepository).delete(cliente);
    }

    @Test
    @DisplayName("Test para eliminar cliente, si no existe, lanza excepcion")
    void eliminarCliente_noExiste_lanzaException() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.eliminarCliente(1L));

        assertEquals("No existe cliente con id 1", ex.getMessage());
        verify(clienteRepository, never()).delete(any(Cliente.class));
    }

    @Test
    @DisplayName("Test para verificar la disponibilidad del correo electronico")
    void checkCorreoAvailability_correoDisponible() {
        when(checker.check(any(Supplier.class), eq("Correo"), eq("test@example.com")))
                .thenReturn(new AvailabilityResponse(true, "Correo"));

        AvailabilityResponse response = clienteService.checkCorreoAvailability("test@example.com");

        assertTrue(response.isAvailable());
        assertEquals("Correo", response.getMessage());
        verify(checker).check(any(Supplier.class), eq("Correo"), eq("test@example.com"));
    }


    @Test
    @DisplayName("Test para obtener cantidad de clientes activos, devuelve cantidad")
    void getCantidadClientesActivos_retornaCantidad() {
        when(clienteRepository.countClienteByActivoTrue()).thenReturn(42.0);

        Map<String, Double> result = clienteService.getCantidadClientesActivos();

        assertNotNull(result);
        assertEquals(42.0, result.get("cantidadClientesActivos"));
    }

    @Test
    @DisplayName("Test para obtener cliente por filtro, paginado")
    void getClientesFiltradosPaginados_deberiaRetornarPagina() {
        ClienteFilterDTO filterDTO = new ClienteFilterDTO();

        Page<Cliente> pageCliente = new PageImpl<>(List.of(cliente));
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageCliente);
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);

        var paginacionResponse = clienteService.getClientesFiltradosPaginados(0, 10, "nombre", filterDTO);

        assertNotNull(paginacionResponse);
        assertEquals(1, paginacionResponse.getContent().size());
        verify(clienteRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Test para obtener cliente por rut")
    void obtenerClientePorRut_clienteExiste_retornaClienteResponse() {
        RutRequest rutRequest = new RutRequest();
        rutRequest.setRut("12345678-9");
        when(clienteRepository.findByRutIgnoreCase("12345678-9")).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);
        ClienteResponse result = clienteService.obtenerClientePorRut(rutRequest);
        assertNotNull(result);
        assertEquals("12345678-9", result.getRut());
        verify(clienteRepository).findByRutIgnoreCase("12345678-9");
        verify(clienteMapper).toResponse(cliente);
    }

    @Test
    @DisplayName("Test para obtener cliente por rut, si el rut no existe, lanza excepcion")
    void obtenerClientePorRut_clienteNoExiste_lanzaResourceNotFoundException() {
        RutRequest rutRequest = new RutRequest();
        rutRequest.setRut("00000000-0");

        when(clienteRepository.findByRutIgnoreCase("00000000-0")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.obtenerClientePorRut(rutRequest));

        assertTrue(ex.getMessage().contains("No existe cliente con rut 00000000-0"));
        verify(clienteRepository).findByRutIgnoreCase("00000000-0");
        verifyNoInteractions(clienteMapper);
    }

    @Test
    @DisplayName("Test para obtener cliente por correo")
    void obtenerClientePorCorreo_clienteExiste_retornaClienteResponse() {
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setCorreo("juan@example.com");
        when(clienteRepository.findByCorreoIgnoreCase("juan@example.com")).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);
        ClienteResponse result = clienteService.obtenerClientePorCorreo(emailRequest);
        assertNotNull(result);
        assertEquals("test@example.com", result.getCorreo());
        verify(clienteRepository).findByCorreoIgnoreCase("juan@example.com");
        verify(clienteMapper).toResponse(cliente);
    }

    @Test
    @DisplayName("Test para obtener cliente por correo, si el correo no existe, lanza excepcion")
    void obtenerClientePorCorreo_clienteNoExiste_lanzaResourceNotFoundException() {
        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setCorreo("noexiste@example.com");
        when(clienteRepository.findByCorreoIgnoreCase("noexiste@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> clienteService.obtenerClientePorCorreo(emailRequest));

        assertTrue(ex.getMessage().contains("No existe cliente con correo noexiste@example.com"));
        verify(clienteRepository).findByCorreoIgnoreCase("noexiste@example.com");
        verifyNoInteractions(clienteMapper);
    }

    @Test
    @DisplayName("Test para buscar clientes por nomre parcial, devuelve lista")
    void buscarClientesPorNombreParcial_retornaListaClientes() {
        when(clienteRepository.findByNombreContainingIgnoreCase("Juan")).thenReturn(List.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);

        List<ClienteResponse> result = clienteService.buscarClientesPorNombreParcial("Juan");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Juan", result.get(0).getNombre());
        verify(clienteRepository).findByNombreContainingIgnoreCase("Juan");
        verify(clienteMapper).toResponse(cliente);
    }

    @Test
    @DisplayName("Test para buscar clientes por nomre y apellido parcial, devuelve lista")
    void buscarClientesPorNombreYApellidoParcial_retornaListaClientes() {
        when(clienteRepository.findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase("Juan", "Perez"))
                .thenReturn(List.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(clienteResponse);

        List<ClienteResponse> result = clienteService.buscarClientesPorNombreYApellidoParcial("Juan", "Perez");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Juan", result.get(0).getNombre());
        assertEquals("Perez", result.get(0).getApellido());
        verify(clienteRepository).findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase("Juan", "Perez");
        verify(clienteMapper).toResponse(cliente);
    }

    @Test
    @DisplayName("Test para listar clientes activos, devuelve lista")
    void listarClientesActivos_deberiaRetornarListaClientesActivos() {
        // Preparar lista de clientes activos
        Cliente clienteActivo = new Cliente();
        clienteActivo.setClienteId(1L);
        clienteActivo.setActivo(true);
        clienteActivo.setNombre("Activo");

        ClienteResponse clienteResponseActivo = new ClienteResponse();
        clienteResponseActivo.setClienteId(1L);
        clienteResponseActivo.setNombre("Activo");

        when(clienteRepository.findByActivoTrue()).thenReturn(List.of(clienteActivo));
        when(clienteMapper.toResponse(clienteActivo)).thenReturn(clienteResponseActivo);

        List<ClienteResponse> resultado = clienteService.listarClientesActivos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Activo", resultado.get(0).getNombre());

        verify(clienteRepository).findByActivoTrue();
        verify(clienteMapper).toResponse(clienteActivo);
    }

    @Test
    @DisplayName("Test para listar clientes inactivos, devuelve lista")
    void listarClientesInactivos_deberiaRetornarListaClientesInactivos() {
        // Preparar lista de clientes inactivos
        Cliente clienteInactivo = new Cliente();
        clienteInactivo.setClienteId(2L);
        clienteInactivo.setActivo(false);
        clienteInactivo.setNombre("Inactivo");

        ClienteResponse clienteResponseInactivo = new ClienteResponse();
        clienteResponseInactivo.setClienteId(2L);
        clienteResponseInactivo.setNombre("Inactivo");

        when(clienteRepository.findByActivoFalse()).thenReturn(List.of(clienteInactivo));
        when(clienteMapper.toResponse(clienteInactivo)).thenReturn(clienteResponseInactivo);

        List<ClienteResponse> resultado = clienteService.listarClientesInactivos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Inactivo", resultado.get(0).getNombre());

        verify(clienteRepository).findByActivoFalse();
        verify(clienteMapper).toResponse(clienteInactivo);
    }
}
