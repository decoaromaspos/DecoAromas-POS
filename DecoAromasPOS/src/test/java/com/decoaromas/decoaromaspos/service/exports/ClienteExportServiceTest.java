package com.decoaromas.decoaromaspos.service.exports;

import com.decoaromas.decoaromaspos.model.Cliente;
import com.decoaromas.decoaromaspos.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteExportServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteExportService exportService;

    @Test
    void escribirClientesACsv_deberiaEscribirCorrectamente() {
        // Arrange
        Cliente cliente = new Cliente();
        cliente.setNombre("Test");
        cliente.setActivo(true);
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        // Act
        exportService.escribirClientesACsv(printWriter);

        // Assert
        String resultado = stringWriter.toString();
        assertTrue(resultado.contains("Test"));
        assertTrue(resultado.contains("Activo"));
        verify(clienteRepository).findAll();
    }

    @Test
    void escribirClientesACsv_DeberiaManejarClientesInactivosYNulls() {
        // Arrange
        Cliente c1 = new Cliente();
        c1.setNombre("Activo");
        c1.setActivo(true);

        Cliente c2 = new Cliente();
        c2.setNombre("Inactivo");
        c2.setActivo(false);

        when(clienteRepository.findAll()).thenReturn(List.of(c1, c2));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Act
        exportService.escribirClientesACsv(pw);

        // Assert
        String res = sw.toString();
        assertTrue(res.contains("Activo"), "Debe contener texto Activo");
        assertTrue(res.contains("Inactivo"), "Debe contener texto Inactivo");
    }
}
