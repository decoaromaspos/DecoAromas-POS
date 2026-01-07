package com.decoaromas.decoaromaspos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrinterService {

    private static final Charset PRINTER_CHARSET = Charset.forName("CP850"); // cambiar si hace falta
    private final ConfiguracionService configuracionService;

    // Comandos ESC/POS comunes
    private static final byte[] INIT = new byte[]{0x1B, 0x40};                     // ESC @
    private static final byte[] NEW_LINE = new byte[]{0x0A};                       // LF
    private static final byte[] CUT_FULL = new byte[]{0x1D, 0x56, 0x00};           // GS V 0
    private static final byte[] CUT_PARTIAL = new byte[]{0x1D, 0x56, 0x01};        // GS V 1
    private static final byte[] OPEN_CASH_DRAWER = new byte[]{0x1B, 0x70, 0x00, 0x3C, (byte)0xFF}; // ESC p m t1 t2
    private static final byte[] BOLD_ON = new byte[]{0x1B, 0x45, 0x01};            // ESC E n (n=1 on)
    private static final byte[] BOLD_OFF = new byte[]{0x1B, 0x45, 0x00};           // ESC E n (n=0 off)
    private static final byte[] ALIGN_LEFT = new byte[]{0x1B, 0x61, 0x00};         // ESC a 0
    private static final byte[] ALIGN_CENTER = new byte[]{0x1B, 0x61, 0x01};       // ESC a 1
    private static final byte[] ALIGN_RIGHT = new byte[]{0x1B, 0x61, 0x02};        // ESC a 2
    private static final byte[] TEXT_NORMAL = new byte[]{0x1B, 0x21, 0x00};        // ESC ! n (font/size)
    private static final byte[] TEXT_DOUBLE_HEIGHT = new byte[]{0x1B, 0x21, 0x10}; // example
    private static final byte[] TEXT_DOUBLE_WIDTH = new byte[]{0x1B, 0x21, 0x20};  // example
    private static final byte[] TEXT_DOUBLE_BOTH = new byte[]{0x1B, 0x21, 0x30};   // example

    // Ajustes de impresora (ajustá PRINTER_DOTS_WIDTH según tu modelo)
    private static final int PRINTER_DOTS_WIDTH = 384;        // ancho en px (ej. 384)
    private static final int LOGO_MAX_HEIGHT = 80;           // altura máxima del logo en px (ajustable)

    public void printReceipt(int port, List<String> lines) throws IOException {
        String ip = configuracionService.getIpImpresora(null);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 3000);
            try (OutputStream out = socket.getOutputStream()) {
                // Inicializa
                out.write(INIT);

                // Imprimir logo (recortado y escalado)
                BufferedImage rawLogo = loadLogo();
                if (rawLogo != null) {
                    BufferedImage cropped = trimWhitespace(rawLogo);
                    BufferedImage resized = resizeToWidthWithMaxHeight(cropped, PRINTER_DOTS_WIDTH, LOGO_MAX_HEIGHT);
                    BufferedImage mono = toMonochromeDither(resized);
                    out.write(ALIGN_CENTER);
                    printRasterImage(out, mono);
                    out.write(NEW_LINE);
                }

                // Centro el texto para mejor estética
                out.write(ALIGN_CENTER);

                // Encabezado centrado y en doble tamaño (primera línea)
                out.write(TEXT_DOUBLE_BOTH);
                writeText(out, lines.size() > 0 ? lines.get(0) : "");
                out.write(NEW_LINE);

                // volver a texto normal
                out.write(TEXT_NORMAL);
                out.write(BOLD_OFF);

                // resto del contenido (ahora centrado)
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i);
                    writeText(out, line);
                    out.write(NEW_LINE);
                }

                // extra feed
                out.write(new byte[]{0x1B, 0x64, 0x03}); // ESC d n (feed n lines)
                // corte
                out.write(CUT_FULL);

                // opcional: abrir cajón
                // out.write(OPEN_CASH_DRAWER);

                out.flush();
            }
        }
    }

    private void writeText(OutputStream out, String text) throws IOException {
        if (text == null) text = "";
        out.write(text.getBytes(PRINTER_CHARSET));
    }

    // -------------------- Helpers del logo --------------------

    private BufferedImage loadLogo() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("logo.png")) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img == null) {
                    System.err.println("Logo cargado pero ImageIO devolvió null.");
                    return null;
                }
                // Si tiene canal alfa, compositar sobre fondo blanco
                if (img.getColorModel().hasAlpha()) {
                    BufferedImage opaque = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = opaque.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, opaque.getWidth(), opaque.getHeight());
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                    return opaque;
                }
                return img;
            } else {
                System.err.println("Logo no encontrado en resources/logo.png.");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error al cargar el logo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Recorta márgenes blancos alrededor de la imagen para reducir espacio superior e inferior.
     */
    private BufferedImage trimWhitespace(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int top = 0;
        int bottom = h - 1;
        int left = 0;
        int right = w - 1;
        boolean found;

        // top
        found = false;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y) & 0xFFFFFF;
                if (rgb != 0xFFFFFF) { top = y; found = true; break; }
            }
            if (found) break;
        }
        // bottom
        found = false;
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y) & 0xFFFFFF;
                if (rgb != 0xFFFFFF) { bottom = y; found = true; break; }
            }
            if (found) break;
        }
        // left
        found = false;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = src.getRGB(x, y) & 0xFFFFFF;
                if (rgb != 0xFFFFFF) { left = x; found = true; break; }
            }
            if (found) break;
        }
        // right
        found = false;
        for (int x = w - 1; x >= 0; x--) {
            for (int y = 0; y < h; y++) {
                int rgb = src.getRGB(x, y) & 0xFFFFFF;
                if (rgb != 0xFFFFFF) { right = x; found = true; break; }
            }
            if (found) break;
        }

        int newW = Math.max(1, right - left + 1);
        int newH = Math.max(1, bottom - top + 1);
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newW, newH);
        g.drawImage(src, 0, 0, newW, newH, left, top, right + 1, bottom + 1, null);
        g.dispose();
        return out;
    }

    /**
     * Redimensiona manteniendo aspecto al ancho target y limitando la altura máxima.
     */
    private BufferedImage resizeToWidthWithMaxHeight(BufferedImage src, int targetWidth, int maxHeight) {
        if (src.getWidth() == targetWidth && src.getHeight() <= maxHeight) return src;
        int targetHeight = (int) Math.round((double) src.getHeight() * targetWidth / src.getWidth());
        if (targetHeight > maxHeight) {
            targetHeight = maxHeight;
            targetWidth = (int) Math.round((double) src.getWidth() * maxHeight / src.getHeight());
        }
        Image scaled = src.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, targetWidth, targetHeight);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return result;
    }

    /**
     * Floyd-Steinberg dithering a 1-bit (mono). Resultado: imagen con TYPE_BYTE_BINARY.
     */
    private BufferedImage toMonochromeDither(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage mono = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        double[][] lum = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                lum[y][x] = 0.299 * r + 0.587 * g + 0.114 * b;
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int oldVal = (int) Math.round(lum[y][x]);
                int newVal = oldVal < 128 ? 0 : 255;
                int err = oldVal - newVal;
                int rgbOut = (newVal == 0) ? 0xFF000000 : 0xFFFFFFFF;
                mono.setRGB(x, y, rgbOut);

                if (x + 1 < w) lum[y][x + 1] += err * 7.0 / 16.0;
                if (y + 1 < h) {
                    if (x - 1 >= 0) lum[y + 1][x - 1] += err * 3.0 / 16.0;
                    lum[y + 1][x] += err * 5.0 / 16.0;
                    if (x + 1 < w) lum[y + 1][x + 1] += err * 1.0 / 16.0;
                }
            }
        }
        return mono;
    }

    /**
     * Imprime imagen monocroma usando GS v 0 raster: 1D 76 30 00 xL xH yL yH [data]
     */
    private void printRasterImage(OutputStream out, BufferedImage image) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        int bytesPerRow = (width + 7) / 8;

        byte[] header = new byte[]{
                0x1D, 0x76, 0x30, 0x00,
                (byte) (bytesPerRow & 0xFF), (byte) ((bytesPerRow >> 8) & 0xFF),
                (byte) (height & 0xFF), (byte) ((height >> 8) & 0xFF)
        };
        out.write(header);

        for (int y = 0; y < height; y++) {
            for (int bx = 0; bx < bytesPerRow; bx++) {
                int b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int x = bx * 8 + bit;
                    int bitVal = 0;
                    if (x < width) {
                        int rgb = image.getRGB(x, y) & 0xFFFFFF;
                        bitVal = (rgb != 0xFFFFFF) ? 1 : 0;
                    }
                    b = (b << 1) | (bitVal & 0x1);
                }
                out.write((byte) b);
            }
        }
        out.write(NEW_LINE);
    }
}