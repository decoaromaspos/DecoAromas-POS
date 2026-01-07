package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.model.Configuracion;
import com.decoaromas.decoaromaspos.repository.ConfiguracionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock
    private ConfiguracionRepository configuracionRepository;
    @InjectMocks
    private ConfiguracionService configuracionService;
    private Configuracion configMetaMensual;
    private Configuracion configIpImpresora;

    @BeforeEach
    void setUp() {
        configMetaMensual = new Configuracion();
        configMetaMensual.setClave("META_MENSUAL");
        configMetaMensual.setValor("150000.50");

        configIpImpresora = new Configuracion();
        configIpImpresora.setClave("IP_IMPRESORA");
        configIpImpresora.setValor("192.168.0.20");
    }

    @Test
    @DisplayName("Test para obtener la meta mensual cuando la configuracion existe")
    void getMetaMensual_DebeRetornarValorExistente() {
        when(configuracionRepository.findByClave("META_MENSUAL"))
                .thenReturn(Optional.of(configMetaMensual));

        BigDecimal result = configuracionService.getMetaMensual(BigDecimal.ZERO);

        assertEquals(new BigDecimal("150000.50"), result);
        verify(configuracionRepository).findByClave("META_MENSUAL");
    }

    @Test
    @DisplayName("Test para obtener la meta mensual cuando no existe, debe retornar el valor por defecto")
    void getMetaMensual_ClaveInexistente_DebeRetornarValorPorDefecto() {
        when(configuracionRepository.findByClave("META_MENSUAL"))
                .thenReturn(Optional.empty());

        BigDecimal result = configuracionService.getMetaMensual(new BigDecimal("9999"));

        assertEquals(new BigDecimal("9999"), result);
    }

    @Test
    @DisplayName("Test para obtener la meta mensual cuando el valor guardado no es un numero valido, debe retonar el valor por defecto")
    void getMetaMensual_ValorInvalido_DebeRetornarPorDefecto() {
        configMetaMensual.setValor("invalido");
        when(configuracionRepository.findByClave("META_MENSUAL"))
                .thenReturn(Optional.of(configMetaMensual));

        BigDecimal result = configuracionService.getMetaMensual(new BigDecimal("123"));

        assertEquals(new BigDecimal("123"), result);
    }

    @Test
    @DisplayName("Test para obtener la IP de la impresora cuando la configuracion existe")
    void getIpImpresora_DebeRetornarValorExistente() {
        when(configuracionRepository.findByClave("IP_IMPRESORA"))
                .thenReturn(Optional.of(configIpImpresora));

        String result = configuracionService.getIpImpresora("127.0.0.1");

        assertEquals("192.168.0.20", result);
    }

    @Test
    @DisplayName("Test para obtener la IP de la impresora cuando no existe, debe retonar el valor por defecto")
    void getIpImpresora_ClaveInexistente_DebeRetornarPorDefecto() {
        when(configuracionRepository.findByClave("IP_IMPRESORA"))
                .thenReturn(Optional.empty());

        String result = configuracionService.getIpImpresora("127.0.0.1");

        assertEquals("127.0.0.1", result);
    }

    @Test
    @DisplayName("Test para guardar una configuracion existente actualizando su valor")
    void guardarConfiguracion_CuandoExiste_DebeActualizarYGuardar() {
        Configuracion existente = new Configuracion("IP_IMPRESORA", "192.168.0.10", "Desc");
        when(configuracionRepository.findByClave("IP_IMPRESORA"))
                .thenReturn(Optional.of(existente));
        when(configuracionRepository.save(existente)).thenReturn(existente);

        Configuracion result = configuracionService.guardarConfiguracion("IP_IMPRESORA", "192.168.0.99");

        assertEquals("192.168.0.99", result.getValor());
        verify(configuracionRepository).save(existente);
    }

    @Test
    @DisplayName("Test para guardar una configuracion nueva cuando la clave no existe")
    void guardarConfiguracion_CuandoNoExiste_DebeCrearNueva() {
        when(configuracionRepository.findByClave("NUEVA_CLAVE"))
                .thenReturn(Optional.empty());

        Configuracion nueva = new Configuracion("NUEVA_CLAVE", "valor123", "");
        when(configuracionRepository.save(any(Configuracion.class))).thenReturn(nueva);

        Configuracion result = configuracionService.guardarConfiguracion("NUEVA_CLAVE", "valor123");

        assertEquals("NUEVA_CLAVE", result.getClave());
        assertEquals("valor123", result.getValor());
        verify(configuracionRepository).save(any(Configuracion.class));
    }
}
