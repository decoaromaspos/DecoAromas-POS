package com.decoaromas.decoaromaspos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrinterServiceTest {

    @Mock
    private ConfiguracionService configuracionService;
    private ByteArrayOutputStream fakeOut;
    private Socket mockSocket;
    private PrinterService printerService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        fakeOut = new ByteArrayOutputStream();
        mockSocket = mock(Socket.class);

        lenient().when(configuracionService.getIpImpresora(null)).thenReturn("127.0.0.1");
        lenient().when(mockSocket.getOutputStream()).thenReturn(fakeOut);

        printerService = spy(new PrinterService(configuracionService));
    }

    @Test
    @DisplayName("Test para verificar que se eliminen correctamente los bordes blancos de una imagen")
    void testTrimWhitespace_removesWhiteBorders() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 10, 10);
        g.setColor(Color.BLACK);
        g.fillRect(3, 3, 4, 4);
        g.dispose();

        var m = PrinterService.class.getDeclaredMethod("trimWhitespace", BufferedImage.class);
        m.setAccessible(true);
        BufferedImage trimmed = (BufferedImage) m.invoke(printerService, img);

        assertEquals(4, trimmed.getWidth());
        assertEquals(4, trimmed.getHeight());
    }

    @Test
    @DisplayName("Test para validar que el método de redimensionado mantiene la proporción de la imagen dentro del alto máximo permitido")
    void testResizeToWidthWithMaxHeight_maintainsAspectRatio() {
        BufferedImage img = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        BufferedImage resized = invokeResize(img, 50, 30);

        assertEquals(50, resized.getWidth());
        assertTrue(resized.getHeight() <= 30);
    }

    @Test
    @DisplayName("Test para comprobar que la conversión a monocromo genera una imagen binaria en escala de grises")
    void testToMonochromeDither_returnsBinaryImage() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 10, 10);
        g.dispose();

        BufferedImage mono = invokeDither(img);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, mono.getType());
    }

    @Test
    @DisplayName("Test para confirmar que el servicio de impresión envía correctamente los comandos ESC/POS al socket")
    void testPrintReceipt_sendsCommandsToSocket() throws Exception {
        when(configuracionService.getIpImpresora(null)).thenReturn("127.0.0.1");

        ByteArrayOutputStream fakeOut = new ByteArrayOutputStream();

        try (MockedConstruction<Socket> mocked = mockConstruction(Socket.class, (mockSocket, context) -> {
            // lo que pasa cada vez que se llama new Socket()
            when(mockSocket.getOutputStream()).thenReturn(fakeOut);
        })) {
            printerService.printReceipt(9100, List.of("FACTURA #001", "Gracias por su compra"));

            byte[] output = fakeOut.toByteArray();
            String sent = new String(output);

            assertTrue(containsSequence(output, new byte[]{0x1B, 0x40}), "Debe enviar comando INIT ESC @");
            assertTrue(sent.contains("FACTURA"), "Debe contener el texto de la factura");
            assertTrue(containsSequence(output, new byte[]{0x1D, 0x56, 0x00}), "Debe cortar el papel");
        }

        verify(configuracionService, times(1)).getIpImpresora(null);
    }


    // -------------------- Helpers privados --------------------

    /**
     * Helper que verifica si un arreglo de bytes contiene una secuencia específica.
     * Usado para validar comandos ESC/POS enviados al flujo de salida.
     */
    private boolean containsSequence(byte[] data, byte[] seq) {
        outer: for (int i = 0; i < data.length - seq.length + 1; i++) {
            for (int j = 0; j < seq.length; j++) {
                if (data[i + j] != seq[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    /**
     * Helper que invoca por reflexión el método privado `resizeToWidthWithMaxHeight`
     * del servicio PrinterService, para probar el redimensionado de imágenes
     * manteniendo la proporción y respetando el alto máximo.
     */
    private BufferedImage invokeResize(BufferedImage img, int w, int h) {
        try {
            var m = PrinterService.class.getDeclaredMethod("resizeToWidthWithMaxHeight", BufferedImage.class, int.class, int.class);
            m.setAccessible(true);
            return (BufferedImage) m.invoke(printerService, img, w, h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper que invoca por reflexión el método privado `toMonochromeDither`
     * del servicio PrinterService, para probar la conversión a imagen binaria.
     */
    private BufferedImage invokeDither(BufferedImage img) {
        try {
            var m = PrinterService.class.getDeclaredMethod("toMonochromeDither", BufferedImage.class);
            m.setAccessible(true);
            return (BufferedImage) m.invoke(printerService, img);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}