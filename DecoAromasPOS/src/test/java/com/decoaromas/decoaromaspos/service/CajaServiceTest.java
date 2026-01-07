package com.decoaromas.decoaromaspos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decoaromas.decoaromaspos.dto.caja.*;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.enums.EstadoCaja;
import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.enums.Rol;
import com.decoaromas.decoaromaspos.exception.BusinessException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.CajaMapper;
import com.decoaromas.decoaromaspos.mapper.UsuarioMapper;
import com.decoaromas.decoaromaspos.model.Caja;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.repository.CajaRepository;
import com.decoaromas.decoaromaspos.repository.PagoVentaRepository;
import com.decoaromas.decoaromaspos.repository.UsuarioRepository;
import com.decoaromas.decoaromaspos.repository.VentaRepository;
import com.decoaromas.decoaromaspos.utils.AvailabilityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock
    private CajaRepository cajaRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private PagoVentaRepository pagoVentaRepository;

    @Mock
    private CajaMapper cajaMapper;

    @InjectMocks
    private CajaService cajaService;

    private Caja cajaAbierta;
    private Caja cajaCerrada;

    @BeforeEach
    void setUp() {
        cajaAbierta = Caja.builder()
                .cajaId(1L)
                .estado(EstadoCaja.ABIERTA)
                .efectivoApertura(100.0)
                .fechaApertura(ZonedDateTime.now().minusHours(2))
                .build();

        cajaCerrada = Caja.builder()
                .cajaId(2L)
                .estado(EstadoCaja.CERRADA)
                .efectivoApertura(200.0)
                .fechaApertura(ZonedDateTime.now().minusDays(1))
                .fechaCierre(ZonedDateTime.now().minusHours(1))
                .efectivoCierre(300.0)
                .build();
    }

    @Test
    @DisplayName("Test para listar cajas, debe retornar una lista de cajas")
    void listarCajas_deberiaRetornarListaDeCajaResponse() {
        List<Caja> cajas = Arrays.asList(cajaAbierta, cajaCerrada);

        CajaResponse responseAbierta = mockCajaResponseFromCaja(cajaAbierta);
        CajaResponse responseCerrada = mockCajaResponseFromCaja(cajaCerrada);

        when(cajaRepository.findAll()).thenReturn(cajas);
        when(cajaMapper.toResponse(cajaAbierta)).thenReturn(responseAbierta);
        when(cajaMapper.toResponse(cajaCerrada)).thenReturn(responseCerrada);

        List<CajaResponse> resultado = cajaService.listarCajas();

        assertEquals(2, resultado.size());
        verify(cajaRepository).findAll();
        verify(cajaMapper, times(2)).toResponse(any());
    }

    @Test
    @DisplayName("Test para abrir caja, si una caja esta abierta y existe, debe lanzar la excepcion correspondiente")
    void abrirCaja_siCajaAbiertaExiste_deberiaLanzarBusinessException() {
        AbrirCajaRequest request = new AbrirCajaRequest();
        request.setEfectivoApertura(100.0);
        request.setUsuarioId(1L);

        when(cajaRepository.existsByEstado(EstadoCaja.ABIERTA)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> cajaService.abrirCaja(request));
        assertEquals("Ya existe una caja abierta. Debe cerrarla antes de abrir una nueva.", ex.getMessage());
    }

    @Test
    @DisplayName("Test para cerrar caja, si el efectivo es nulo, debe lanzar BusinessException")
    void cerrarCaja_efectivoNull_deberiaLanzarBusinessException() {

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> cajaService.cerrarCaja(null)
        );

        assertEquals(
                "Debe ingresar el efectivo real contado para cerrar la caja.",
                ex.getMessage()
        );
    }


    @Test
    @DisplayName("Test para obtener una caja, si una caja esta abierta y existe, debe retornar la caja")
    void obtenerCajaRealAbierta_existente_deberiaRetornarCaja() {
        when(cajaRepository.findByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.of(cajaAbierta));

        Caja caja = cajaService.obtenerCajaRealAbierta();

        assertNotNull(caja);
        assertEquals(EstadoCaja.ABIERTA, caja.getEstado());
    }

    @Test
    @DisplayName("Test para obtener una caja, si una caja esta abierta y no existe, debe retornar una excepcion")
    void obtenerCajaRealAbierta_noExistente_deberiaLanzarResourceNotFound() {
        lenient().when(cajaRepository.findByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cajaService.obtenerCajaRealAbierta());
    }

    @Test
    @DisplayName("Test para calcular un resumen de caja")
    void calcularResumenCaja_deberiaCalcularCorrectamente() {

        when(pagoVentaRepository.sumTotalesByCajaId(1L)).thenReturn(List.of(
                new PagoPorMedioDTO(MedioPago.EFECTIVO, 500.0),
                new PagoPorMedioDTO(MedioPago.BCI, 300.0)
        ));

        CajaResumenResponse resumen = cajaService.calcularResumenCaja(cajaAbierta);

        assertEquals(500.0, resumen.getTotalEfectivo());
        assertEquals(0.0, resumen.getTotalMercadoPago());
        assertEquals(300.0, resumen.getTotalBCI());
        assertEquals(0.0, resumen.getTotalBotonDePago());
        assertEquals(0.0, resumen.getTotalTransferencia());
    }


    @Test
    @DisplayName("Test para obtener caja existente por ID")
    void obtenerCajaPorId_existente_deberiaRetornarCajaResponse() {
        CajaResponse responseMock = mockCajaResponseFromCaja(cajaAbierta);

        when(cajaRepository.findById(1L)).thenReturn(Optional.of(cajaAbierta));
        when(cajaMapper.toResponse(cajaAbierta)).thenReturn(responseMock);

        CajaResponse response = cajaService.obtenerCajaPorId(1L);

        assertNotNull(response);
        verify(cajaRepository).findById(1L);
        verify(cajaMapper).toResponse(cajaAbierta);
    }

    @Test
    @DisplayName("Test para abrir una caja, caja no abierta debe guardarse")
    void abrirCaja_cajaNoAbierta_deberiaGuardarYRetornarCajaResponse() {
        AbrirCajaRequest request = new AbrirCajaRequest();
        request.setEfectivoApertura(150.0);
        request.setUsuarioId(1L);

        CajaResponse responseMock = CajaResponse.builder()
                .cajaId(1L)
                .fechaApertura(ZonedDateTime.now())
                .efectivoApertura(150.0)
                .fechaCierre(null)
                .efectivoCierre(null)
                .mercadoPagoCierre(null)
                .bciCierre(null)
                .botonDePagoCierre(null)
                .transferenciaCierre(null)
                .estado(EstadoCaja.ABIERTA)
                .diferenciaReal(null)
                .usuarioId(1L)
                .nombreUsuario("")
                .username("")
                .build();

        when(cajaRepository.existsByEstado(EstadoCaja.ABIERTA)).thenReturn(false);
        when(usuarioService.obtenerUsuarioRealPorId(1L)).thenReturn(mockUser());

        when(cajaRepository.save(any(Caja.class))).thenAnswer(invocation -> {
            Caja cajaGuardada = invocation.getArgument(0);
            cajaGuardada.setCajaId(1L);
            return cajaGuardada;
        });
        when(cajaMapper.toResponse(any(Caja.class))).thenReturn(responseMock);

        CajaResponse response = cajaService.abrirCaja(request);

        assertNotNull(response);
        verify(cajaRepository).existsByEstado(EstadoCaja.ABIERTA);
        verify(usuarioService).obtenerUsuarioRealPorId(1L);
        verify(cajaRepository).save(any(Caja.class));
        verify(cajaMapper).toResponse(any(Caja.class));
    }

    @Test
    @DisplayName("Test para cambiar estado activo, un admin puede gestionarse a sí mismo")
    void cambiarEstadoActivo_adminPuedeGestionarseASiMismo() {

        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("admin");
        actor.setRol(Rol.ADMIN);
        actor.setActivo(true);

        Usuario target = new Usuario();
        target.setUsuarioId(1L);
        target.setUsername("admin");
        target.setRol(Rol.ADMIN);
        target.setActivo(true);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(mockRepo.findByUsernameIgnoreCase("admin"))
                .thenReturn(Optional.of(actor));
        when(mockRepo.findById(1L))
                .thenReturn(Optional.of(target));
        when(mockRepo.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        assertDoesNotThrow(() -> service.cambiarEstadoActivo(1L, false));

        verify(mockRepo).save(any(Usuario.class));

        SecurityContextHolder.clearContext();
    }



    @Test
    @DisplayName("Test para obteener caja por fiiltro, paginado")
    void getCajasFiltradas_deberiaRetornarPaginacionResponse() {
        LocalDate inicio = LocalDate.now().minusDays(2);
        LocalDate fin = LocalDate.now();

        Page<Caja> pageCaja = new PageImpl<>(List.of(cajaAbierta));
        CajaResponse responseMock = mockCajaResponseFromCaja(cajaAbierta);

        when(cajaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageCaja);
        when(cajaMapper.toResponse(any(Caja.class))).thenReturn(responseMock);

        PaginacionResponse<CajaResponse> paginacion = cajaService.getCajasFiltradas(
                0, 10, "fechaApertura", inicio, fin, null, null, null);

        assertNotNull(paginacion);
        assertEquals(1, paginacion.getContent().size());
        verify(cajaRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Test para obtener resumen de caja abierta")
    void obtenerResumenCajaAbierta_deberiaRetornarResumen() {
        when(cajaRepository.findByEstado(EstadoCaja.ABIERTA)).thenReturn(Optional.of(cajaAbierta));

        CajaResumenResponse resumenMock = new CajaResumenResponse(100.0, 200.0, 300.0, 400.0, 500.0, 75.0);

        CajaService cajaServiceSpy = Mockito.spy(cajaService);
        doReturn(resumenMock).when(cajaServiceSpy).calcularResumenCaja(cajaAbierta);

        CajaResumenResponse resultado = cajaServiceSpy.obtenerResumenCajaAbierta();

        assertNotNull(resultado);
        assertEquals(100.0, resultado.getTotalEfectivo());
        assertEquals(200.0, resultado.getTotalMercadoPago());

        verify(cajaRepository).findByEstado(EstadoCaja.ABIERTA);
        verify(cajaServiceSpy).calcularResumenCaja(cajaAbierta);
    }

    @Test
    @DisplayName("Test para eliminar caja")
    void eliminarCaja_existente_deberiaEliminarCaja() {
        when(cajaRepository.findById(1L)).thenReturn(Optional.of(cajaAbierta));
        doNothing().when(cajaRepository).delete(cajaAbierta);

        cajaService.eliminarCaja(1L);

        verify(cajaRepository).findById(1L);
        verify(cajaRepository).delete(cajaAbierta);
    }

    @Test
    @DisplayName("Test para eliminar caja no existente, debe lanzar excepcion")
    void eliminarCaja_noExiste_deberiaLanzarException() {
        when(cajaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cajaService.eliminarCaja(1L));

        verify(cajaRepository).findById(1L);
        verify(cajaRepository, never()).delete((Caja) any());


    }


    // Helper para construir un CajaResponse sencillo desde Caja
    private CajaResponse mockCajaResponseFromCaja(Caja caja) {
        return CajaResponse.builder()
                .cajaId(caja.getCajaId())
                .fechaApertura(caja.getFechaApertura())
                .efectivoApertura(caja.getEfectivoApertura())
                .fechaCierre(caja.getFechaCierre())
                .efectivoCierre(caja.getEfectivoCierre())
                .mercadoPagoCierre(caja.getMercadoPagoCierre())
                .bciCierre(caja.getBciCierre())
                .botonDePagoCierre(caja.getBotonDePagoCierre())
                .transferenciaCierre(caja.getTransferenciaCierre())
                .estado(caja.getEstado())
                .diferenciaReal(caja.getDiferenciaReal())
                .usuarioId(null)
                .nombreUsuario("")
                .username("")
                .build();
    }

    private Usuario mockUser() {
        return mock(Usuario.class);
    }
}
