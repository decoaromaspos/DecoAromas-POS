package com.decoaromas.decoaromaspos.service;


import com.decoaromas.decoaromaspos.enums.TipoDescuento;
import com.decoaromas.decoaromaspos.model.Cliente;
import com.decoaromas.decoaromaspos.model.DetalleVenta;
import com.decoaromas.decoaromaspos.model.PagoVenta;
import com.decoaromas.decoaromaspos.model.Venta;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Servicio dedicado exclusivamente a formatear entidades de negocio
 * (como Venta) en una lista de Strings legibles para un recibo de texto (ticket).
 */
@Service
@RequiredArgsConstructor
public class ReceiptBuilderService {

    // Ancho estándar del ticket en caracteres
    private static final int RECEIPT_WIDTH = 36;

    // Locale para formateo de moneda (CLP usa '.' como separador de miles)
    private static final Locale LOCALE_CL = new Locale("es", "CL");

    /**
     * Construye las líneas de texto a imprimir para un comprobante simple (ticket).
     *
     * @param venta La entidad Venta a imprimir.
     * @return Una lista de strings, donde cada string es una línea del recibo.
     */
    public List<String> buildReceiptLines(Venta venta) {
        // --- 1. Precálculos de Totales ---
        // Obtenemos todos los valores pre-calculados de la Venta.
        double subtotalBruto = (venta.getTotalBruto() != null) ? venta.getTotalBruto() : 0.0;
        double descuentoGlobal = (venta.getMontoDescuentoGlobalCalculado() != null) ? venta.getMontoDescuentoGlobalCalculado() : 0.0;
        double totalNetoVenta = (venta.getTotalNeto() != null) ? venta.getTotalNeto() : 0.0;
        double descuentoUnitarios = (venta.getTotalDescuentosUnitarios() != null) ? venta.getTotalDescuentosUnitarios() : 0.0;

        // Calculamos el total de productos (esto sí requiere iterar)
        int totalCantidad = 0;
        for (DetalleVenta d : venta.getDetalles()) {
            totalCantidad += d.getCantidad();
        }

        // Cálculos de IVA sobre el Total
        // Neto = Total / 1.19
        double netoBase = totalNetoVenta / 1.19;
        // IVA = Total - Neto
        double iva = totalNetoVenta - netoBase;




        // --- 2. Construcción de Líneas ---
        List<String> lines = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm", LOCALE_CL)
                .withZone(DateUtils.ZONE_ID_SANTIAGO);

        lines.add(center("DECOAROMAS S.A.", 15));
        lines.add(center("Fragancias que cautivan", 15));
        lines.add("-".repeat(RECEIPT_WIDTH));
        lines.add("Fecha: " + (venta.getFecha() != null ? formatter.format(venta.getFecha()) : ""));
        lines.add("Vendedor: " + (venta.getUsuario() != null ? venta.getUsuario().getUsername() : "N/A"));
        if (venta.getCliente() != null) {
            Cliente cliente = venta.getCliente();

            if (cliente.getNombre() != null && !cliente.getNombre().isBlank()) {
                lines.add("Cliente: " + cliente.getNombre().trim());
            }
            if (cliente.getRut() != null && !cliente.getRut().isBlank()) {
                lines.add("RUT: " + cliente.getRut().trim());
            }
            if (cliente.getCorreo() != null && !cliente.getCorreo().isBlank()) {
                lines.add("Email: " + cliente.getCorreo().trim());
            }
            if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()) {
                lines.add("Tel: " + cliente.getTelefono().trim());
            }
        }
        lines.add("Tipo de Venta: " + venta.getTipoCliente());
        lines.add("Tipo de Documento: " + venta.getTipoDocumento());
        lines.add("-".repeat(RECEIPT_WIDTH));


        // Cabecera de la tabla
        // Formato: Producto (25), DctoU (5), Monto (6) = 36
        lines.add(String.format("%-25s %5s %6s", "Producto", "DctoU", "Monto"));
        for (DetalleVenta d : venta.getDetalles()) {
            String fullName = d.getProducto().getNombre();
            int nameColWidth = 24; // Dejamos 1 char para el guion
            List<String> nameLines = wrapPreserveWords(fullName, nameColWidth);

            // --- LÓGICA MOSTRAR DESCUENTO UNITARIO ---
            // (Muestra el valor/porcentaje original, ej.: "10%" o "$500")
            String descuentoUStr = "";
            if (d.getValorDescuentoUnitario() != null && d.getValorDescuentoUnitario() > 0) {
                if (d.getTipoDescuentoUnitario() == TipoDescuento.PORCENTAJE) {
                    descuentoUStr = String.format("%.0f%%", d.getValorDescuentoUnitario());// Muestra el porcentaje, ej: "15%"
                } else {
                    descuentoUStr = formatMoney(d.getValorDescuentoUnitario());// Muestra el valor en dinero, ej.: "$100"
                }
            }

            String subtotalStr = formatMoney(d.getSubtotal()); // formatMoney para el subtotal

            // Línea 1: Nombre del producto, descuento y subtotal
            String first = nameLines.size() > 0 ? "-" + padRight(nameLines.get(0), nameColWidth) : "-" + repeat(' ', nameColWidth);
            lines.add(String.format("%-25s %5s %6s", first, descuentoUStr, subtotalStr));


            // líneas siguientes (si el nombre es largo)
            for (int i = 1; i < nameLines.size(); i++) {
                String rest = " " + padRight(nameLines.get(i), nameColWidth);
                lines.add(String.format("%-25s %5s %6s", rest, "", ""));
            }

            // Línea adicional con (Cantidad x $PrecioUnitario)
            String detalleStr = String.format("  %d x %s", d.getCantidad(), formatMoney(d.getPrecioUnitario()));
            lines.add(detalleStr);
        }

        // Formato: Label (28), Valor (8) = 36
        lines.add(String.format("%-28s %8d", "Total productos:", totalCantidad));
        lines.add("-".repeat(RECEIPT_WIDTH));


        // --- 3. Sección de Totales ---

        // Subtotal (Bruto, antes de descuentos)
        lines.add(String.format("%-28s %8s", "Subtotal:", formatMoney(subtotalBruto)));

        // Mostrar Descuentos Unitarios (SOLO SI APLICA)
        if (descuentoUnitarios > 0) {
            lines.add(String.format("%-28s %8s", "Desc. Unitarios:", formatMoney(descuentoUnitarios * -1))); // En negativo
        }

        // Mostrar Descuento Global (SOLO SI APLICA)
        if (descuentoGlobal > 0) {
            String label = "Desc. Global:";
            if (venta.getTipoDescuentoGlobal() == TipoDescuento.PORCENTAJE) {
                label = String.format("Desc. Global (%.0f%%):", venta.getValorDescuentoGlobal());
            }
            lines.add(String.format("%-28s %8s", label, formatMoney(descuentoGlobal * -1))); // En negativo
        }

        lines.add("-".repeat(RECEIPT_WIDTH));

        // Neto
        lines.add(String.format("%-28s %8s", "NETO:", formatMoney(netoBase)));
        // IVA
        lines.add(String.format("%-28s %8s", "IVA (19%):", formatMoney(iva)));

        // Total (Total Neto de la Venta)
        lines.add(String.format("%-28s %8s", "TOTAL:", formatMoney(totalNetoVenta)));
        lines.add("-".repeat(RECEIPT_WIDTH));

        // --- 4. Sección de Pagos ---
        lines.add("Forma de Pago:");
        for (PagoVenta pago : venta.getPagos()) {
            String medioPagoNombre = pago.getMedioPago().getNombreParaUi();
            lines.add(String.format("  %-26s %8s", medioPagoNombre + ":", formatMoney(pago.getMonto()))); // Formato: "  Label:" (28), Valor (8)
        }

        // Mostrar Vuelto si existe (pago en Efectivo)
        if (venta.getVuelto() != null && venta.getVuelto() > 0) {
            lines.add(String.format("  %-26s %8s", "Vuelto:", formatMoney(venta.getVuelto())));
        }

        lines.add("-".repeat(RECEIPT_WIDTH));
        lines.add(center("GRACIAS POR SU COMPRA", RECEIPT_WIDTH));
        lines.add(""); // linea extra para feed

        //for (String line : lines) {System.out.println(line);}
        return lines;
    }

    /** Helper para formatear dinero (ej: $1000) */
    private String formatMoney(Double value) {
        if (value == null) return "$0";

        // Usamos un DecimalFormat con Locale "es_CL" para obtener el punto como separador de miles.
        try {
            DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(LOCALE_CL);
            df.applyPattern("$#,##0;-$#,##0");// Patrón para miles con punto, sin decimales.
            return df.format(value);
        } catch (Exception e) {
            // Fallback por si el Locale falla
            return String.format(Locale.getDefault(), "$%.0f", value);
        }
    }

    /** Helper para truncar valor */
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Helper para centrar texto en el recibo */
    private String center(String s, int width) {
        if (s == null) return "";
        if (s.length() >= width) return s;
        int left = (width - s.length()) / 2;
        return " ".repeat(left) + s;
    }

    /** Helper para ajustar texto (word wrap) */
    private List<String> wrapPreserveWords(String text, int maxWidth) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            parts.add("");
            return parts;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            if (line.length() == 0) {
                if (w.length() <= maxWidth) {
                    line.append(w);
                } else {
                    int idx = 0;
                    while (idx < w.length()) {
                        int end = Math.min(idx + maxWidth, w.length());
                        parts.add(w.substring(idx, end));
                        idx = end;
                    }
                }
            } else {
                if (line.length() + 1 + w.length() <= maxWidth) {
                    line.append(' ').append(w);
                } else {
                    parts.add(line.toString());
                    line.setLength(0);
                    if (w.length() <= maxWidth) {
                        line.append(w);
                    } else {
                        int idx = 0;
                        while (idx < w.length()) {
                            int end = Math.min(idx + maxWidth, w.length());
                            parts.add(w.substring(idx, end));
                            idx = end;
                        }
                    }
                }
            }
        }
        if (line.length() > 0) parts.add(line.toString());
        return parts;
    }

    /** Helper para rellenar texto a la derecha */
    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    /** Helper para repetir un carácter */
    private String repeat(char c, int times) {
        return String.valueOf(c).repeat(Math.max(0, times));
    }
}
