package com.decoaromas.decoaromaspos.service.exports;

import com.decoaromas.decoaromaspos.dto.venta.VentaFilterDTO;
import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import com.decoaromas.decoaromaspos.model.*;
import com.decoaromas.decoaromaspos.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaExportServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private VentaExportService ventaExportService;

    @Test
    void escribirVentasACsv_DeberiaManejarVentasConMultiplesDetallesYPagos() {
        // --- ARRANGE ---
        ZonedDateTime ahora = ZonedDateTime.now();

        // Configurar Venta
        Venta venta = new Venta();
        venta.setVentaId(100L);
        venta.setFecha(ahora);
        venta.setTipoDocumento(TipoDocumento.BOLETA);
        venta.setTotalNeto(5000.0);
        venta.setVuelto(0.0);

        // Detalle (1 producto)
        Producto p = new Producto();
        p.setNombre("Vela Aromática");
        p.setSku("VEL-01");

        DetalleVenta dv = new DetalleVenta();
        dv.setProducto(p);
        dv.setCantidad(1);
        dv.setPrecioUnitario(5000.0);
        venta.setDetalles(List.of(dv));

        // Pagos (2 pagos para forzar la lógica de filas múltiples)
        PagoVenta pago1 = new PagoVenta();
        pago1.setMedioPago(MedioPago.EFECTIVO);
        pago1.setMonto(3000.0);

        PagoVenta pago2 = new PagoVenta();
        pago2.setMedioPago(MedioPago.TRANSFERENCIA);
        pago2.setMonto(2000.0);
        venta.setPagos(List.of(pago1, pago2));

        when(ventaRepository.findAll(any(Specification.class))).thenReturn(List.of(venta));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // --- ACT ---
        ventaExportService.escribirVentasACsv(pw, ahora, ahora, new VentaFilterDTO());

        // --- ASSERT ---
        String csvOutput = sw.toString();

        // Verificar que aparezcan los datos básicos
        assertTrue(csvOutput.contains("100"), "Debe contener el ID de la venta");
        assertTrue(csvOutput.contains("Vela Aromática"), "Debe contener el producto");

        // Verificar que aparezcan ambos medios de pago (esto confirma que el loop de filas funcionó)
        assertTrue(csvOutput.contains("EFECTIVO"));
        assertTrue(csvOutput.contains("TRANSFERENCIA"));

        // Verificar el BOM (UTF-8)
        assertTrue(csvOutput.startsWith("\ufeff"));
    }
}