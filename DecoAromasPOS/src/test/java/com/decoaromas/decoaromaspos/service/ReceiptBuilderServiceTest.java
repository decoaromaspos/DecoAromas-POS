package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import com.decoaromas.decoaromaspos.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReceiptBuilderServiceTest {

    private ReceiptBuilderService receiptBuilderService;
    private ReceiptBuilderService service;
    private Method truncateMethod;
    private Method repeatMethod;
    private Method wrapPreserveWordsMethod;

    @BeforeEach
    void setUp() throws Exception {
        receiptBuilderService = new ReceiptBuilderService();

        truncateMethod = ReceiptBuilderService.class.getDeclaredMethod("truncate", String.class, int.class);
        truncateMethod.setAccessible(true);

        repeatMethod = ReceiptBuilderService.class.getDeclaredMethod("repeat", char.class, int.class);
        repeatMethod.setAccessible(true);

        wrapPreserveWordsMethod = ReceiptBuilderService.class.getDeclaredMethod("wrapPreserveWords", String.class, int.class);
        wrapPreserveWordsMethod.setAccessible(true);
    }

    @Test
    @DisplayName("Test para construir recibo con lineas basicas, venta basica con descuento")
    void buildReceiptLines_basicVenta_conDescuentos() {
        // Preparar datos de prueba
        Venta venta = new Venta();
        venta.setTotalNeto(5980.0);
        venta.setTotalBruto(6990.0);
        venta.setTipoCliente(TipoCliente.DETALLE);
        venta.setTipoDocumento(TipoDocumento.FACTURA);
        venta.setTipoDescuentoGlobal(TipoDescuento.VALOR);
        venta.setValorDescuentoGlobal(0.0);
        venta.setMontoDescuentoGlobalCalculado(0.0);    // Es el moto global que se calcula, no el descuento total (global + unitario)
        venta.setTotalDescuentosUnitarios(1010.0);
        venta.setTotalDescuentoTotal(1010.0);
        venta.setVuelto(14020.0);

        Usuario usuario = new Usuario();
        usuario.setUsername("vendedor01");
        venta.setUsuario(usuario);

        Cliente cliente = new Cliente();
        cliente.setNombre("Juan Perez");
        cliente.setRut("12345678-9");
        cliente.setCorreo("juan@example.com");
        cliente.setTelefono("912345678");
        venta.setCliente(cliente);

        // Fecha fija para test
        venta.setFecha(ZonedDateTime.of(2025, 11, 12, 15, 30, 0, 0, ZoneId.of("America/Santiago")));

        // Detalles de venta
        Producto p1 = new Producto();
        p1.setNombre("Perfume floral intenso");

        DetalleVenta d1 = new DetalleVenta();
        d1.setProducto(p1);
        d1.setCantidad(2);
        d1.setPrecioUnitario(1150.0);
        d1.setSubtotal(2300.0);
        d1.setValorDescuentoUnitario(160.0);
        d1.setTipoDescuentoUnitario(TipoDescuento.VALOR);

        Producto p2 = new Producto();
        p2.setNombre("Jabón artesanal de lavanda");

        DetalleVenta d2 = new DetalleVenta();
        d2.setProducto(p2);
        d2.setCantidad(1);
        d2.setPrecioUnitario(1390.0);
        d2.setSubtotal(1390.0);
        d2.setValorDescuentoUnitario(390.0);
        d2.setTipoDescuentoUnitario(TipoDescuento.VALOR);

        Producto p3 = new Producto();
        p3.setNombre("Mikado auto 50ml");

        DetalleVenta d3 = new DetalleVenta();
        d3.setProducto(p3);
        d3.setCantidad(3);
        d3.setPrecioUnitario(1100.0);
        d3.setSubtotal(3300.0);
        d3.setValorDescuentoUnitario(100.0);
        d3.setTipoDescuentoUnitario(TipoDescuento.VALOR);

        venta.setDetalles(List.of(d1, d2, d3));

        PagoVenta pago1 = new PagoVenta();
        pago1.setMedioPago(MedioPago.EFECTIVO);
        pago1.setMonto(20000.0);

        venta.setPagos(List.of(pago1));

        // Ejecutar método a testear
        List<String> lines = receiptBuilderService.buildReceiptLines(venta);

        // Validar algunas líneas clave
        assertThat(lines).isNotEmpty();

        lines.forEach(System.out::println); // para depurar qué está en lines

        assertThat(lines).anyMatch(line -> line.contains("DECOAROMAS S.A."));
        assertThat(lines).anyMatch(line -> line.contains("Fragancias que cautivan"));
        assertThat(lines).anyMatch(line -> line.contains("Fecha: 2025-11-12 15:30"));
        assertThat(lines).anyMatch(line -> line.contains("Vendedor: vendedor01"));
        assertThat(lines).anyMatch(line -> line.contains("Cliente: Juan Perez"));
        assertThat(lines).anyMatch(line -> line.contains("RUT: 12345678-9"));
        assertThat(lines).anyMatch(line -> line.contains("Email: juan@example.com"));
        assertThat(lines).anyMatch(line -> line.contains("Tel: 912345678"));

        assertThat(lines).anyMatch(line -> line.contains("Tipo de Venta: " + venta.getTipoCliente().name()));
        assertThat(lines).anyMatch(line -> line.contains("Tipo de Documento: " + venta.getTipoDocumento().name()));

        assertThat(lines).anyMatch(line -> line.contains("Perfume floral"));
        assertThat(lines).anyMatch(line -> line.contains("Jabón artesanal"));

        assertThat(lines).anyMatch(line -> line.contains("$160"));

        assertThat(lines).anyMatch(line -> line.matches(".*\\$\\d+.*"));

        assertThat(lines).anyMatch(line -> line.contains("Total productos:") && line.contains("6"));
        assertThat(lines).anyMatch(line -> line.contains("Desc. Unitarios"));
        assertThat(lines).anyMatch(line -> line.contains("TOTAL:"));

        assertThat(lines.get(lines.size() - 2)).contains("GRACIAS POR SU COMPRA");

    }


    // Helpers para invocar métodos privados por reflexión

    @SuppressWarnings("unchecked")
    private List<String> callWrapPreserveWords(String text, int maxWidth) throws Exception {
        return (List<String>) wrapPreserveWordsMethod.invoke(receiptBuilderService, text, maxWidth);
    }

    private String callTruncate(String s, int max) throws Exception {
        return (String) truncateMethod.invoke(receiptBuilderService, s, max);
    }

    private String callRepeat(char c, int times) throws Exception {
        return (String) repeatMethod.invoke(receiptBuilderService, c, times);
    }

    // Tests para truncate
    @Test
    @DisplayName("Test para truncar valores iguales y cortos al original")
    void truncate_shouldReturnOriginalIfShorterOrEqual() throws Exception {
        assertThat(callTruncate(null, 5)).isEqualTo("");
        assertThat(callTruncate("", 5)).isEqualTo("");
        assertThat(callTruncate("abc", 5)).isEqualTo("abc");
        assertThat(callTruncate("hello", 5)).isEqualTo("hello");
    }

    @Test
    @DisplayName("Test para truncar debe truncar el valor y añadir un mensaje")
    void truncate_shouldTruncateAndAddEllipsis() throws Exception {
        assertThat(callTruncate("abcdef", 5)).isEqualTo("abcd…"); // 4 chars + "…"
        assertThat(callTruncate("This is a long string", 10)).isEqualTo("This is a…");
    }

    // Tests para repeat
    @Test
    @DisplayName("Test para repetir un valor n veces")
    void repeat_shouldRepeatCharTimes() throws Exception {
        assertThat(callRepeat('a', 0)).isEqualTo("");
        assertThat(callRepeat('a', -5)).isEqualTo("");
        assertThat(callRepeat('a', 1)).isEqualTo("a");
        assertThat(callRepeat('a', 3)).isEqualTo("aaa");
        assertThat(callRepeat(' ', 4)).isEqualTo("    ");
    }

    // Tests para wrapPreserveWords
    @Test
    @DisplayName("Test para preservar palabras no nulas y vacias, debe devolver la lista vacia en caso correspondiente")
    void wrapPreserveWords_nullOrEmpty_shouldReturnListWithEmptyString() throws Exception {
        assertThat(callWrapPreserveWords(null, 10)).containsExactly("");
        assertThat(callWrapPreserveWords("", 10)).containsExactly("");
    }

    @Test
    @DisplayName("Test para preservar palabras, palabras cortas aplica filtro en una linea")
    void wrapPreserveWords_shortWords_fitInOneLine() throws Exception {
        assertThat(callWrapPreserveWords("Hola mundo", 20)).containsExactly("Hola mundo");
    }

    @Test
    @DisplayName("Test para preservar palabras, excede un limite maximo de palabras por bloque")
    void wrapPreserveWords_wordsWrap_whenExceedsMaxWidth() throws Exception {
        List<String> lines = callWrapPreserveWords("Hola mundo desde ChatGPT", 10);
        assertThat(lines).containsExactly("Hola mundo", "desde", "ChatGPT");
    }

    @Test
    @DisplayName("Test para preservar palabras, palabras largas, se aplica un corte")
    void wrapPreserveWords_longWord_splitInChunks() throws Exception {
        List<String> lines = callWrapPreserveWords("supercalifragilisticexpialidocious", 10);
        assertThat(lines).containsExactly(
                "supercalif",
                "ragilistic",
                "expialidoc",
                "ious"
        );
    }

    @Test
    @DisplayName("Test para preservar palabras, max de longitud y palabras cortas")
    void wrapPreserveWords_mixLongAndShortWords() throws Exception {
        List<String> lines = callWrapPreserveWords("Hola supercalifragilisticexpialidocious mundo", 10);
        assertThat(lines.get(0)).isEqualTo("Hola");
        assertThat(lines.subList(1, 5)).containsExactly(
                "supercalif",
                "ragilistic",
                "expialidoc",
                "ious"
        );
        assertThat(lines.get(5)).isEqualTo("mundo");
    }
}
