package com.decoaromas.decoaromaspos.service.exports;

import com.decoaromas.decoaromaspos.dto.venta.VentaFilterDTO;
import com.decoaromas.decoaromaspos.exception.ExportException;
import com.decoaromas.decoaromaspos.model.DetalleVenta;
import com.decoaromas.decoaromaspos.model.PagoVenta;
import com.decoaromas.decoaromaspos.model.Venta;
import com.decoaromas.decoaromaspos.repository.VentaRepository;
import com.decoaromas.decoaromaspos.utils.DateUtils;
import com.decoaromas.decoaromaspos.utils.VentaSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaExportService {

    private final VentaRepository ventaRepository;

    @Transactional(readOnly = true)
    public void escribirVentasACsv(PrintWriter writer, ZonedDateTime inicio, ZonedDateTime fin, VentaFilterDTO filtros) {
        writer.write('\ufeff');

        Specification<Venta> spec = VentaSpecification.conFiltros(inicio, fin, filtros);
        List<Venta> ventas = new ArrayList<>(ventaRepository.findAll(spec)); // Crear ArrayList mutable
        ventas.sort(Comparator.comparing(Venta::getFecha));

        String[] headers = {
                "ID Venta", "Fecha", "Tipo", "Cliente", "RUT", "Ciudad", "Tipo Doc", "Nro Doc",
                "Producto", "SKU", "Cant", "Precio Unit",
                "Pago Método", "Pago Monto", "Vuelto", "Total Venta"
        };
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
            for (Venta v : ventas) {
                String fechaStr = v.getFecha()
                        .withZoneSameInstant(DateUtils.ZONE_ID_SANTIAGO)
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));

                List<DetalleVenta> detalles = v.getDetalles();
                List<PagoVenta> pagos = v.getPagos();

                // Determinamos cuántas filas ocupará esta venta (el máximo entre productos y pagos)
                int filasVenta = Math.max(detalles.size(), pagos.size());

                for (int i = 0; i < filasVenta; i++) {
                    boolean esPrimeraFila = (i == 0);

                    // Datos de Cabecera (Solo en la primera fila)
                    String fecha = esPrimeraFila ? fechaStr : "";
                    String totalVenta = esPrimeraFila ? String.valueOf(v.getTotalNeto()) : "";
                    String vuelto = esPrimeraFila ? String.valueOf(v.getVuelto()) : "";
                    String tDoc = esPrimeraFila ? String.valueOf(v.getTipoDocumento()) : "";
                    String tVenta = esPrimeraFila ? String.valueOf(v.getTipoCliente()) : "";

                    // Lógica de Cliente
                    String cliente = "";
                    String rut = "";
                    String ciudad = "";
                    if (esPrimeraFila) {
                        cliente = (v.getCliente() != null) ? v.getCliente().getNombre() : "Venta General";
                        rut = (v.getCliente() != null) ? v.getCliente().getRut() : "";
                        ciudad = (v.getCliente() != null) ? v.getCliente().getCiudad() : "";
                    }

                    String nDoc = (esPrimeraFila && v.getNumeroDocumento() != null) ? v.getNumeroDocumento() : "";


                    // Datos de Producto
                    String prodNom = (i < detalles.size()) ? detalles.get(i).getProducto().getNombre() : "";
                    String prodSku = (i < detalles.size()) ? detalles.get(i).getProducto().getSku() : "";
                    String prodCant = (i < detalles.size()) ? String.valueOf(detalles.get(i).getCantidad()) : "";
                    String prodPrec = (i < detalles.size()) ? String.valueOf(detalles.get(i).getPrecioUnitario()) : "";

                    // Datos de Pago
                    String pagoMetodo = (i < pagos.size()) ? pagos.get(i).getMedioPago().toString() : "";
                    String pagoMonto = (i < pagos.size()) ? String.valueOf(pagos.get(i).getMonto()) : "";

                    csvPrinter.printRecord(
                            v.getVentaId(),
                            fecha, tVenta, cliente, rut, ciudad, tDoc, nDoc,
                            prodNom, prodSku, prodCant, prodPrec,
                            pagoMetodo, pagoMonto, vuelto, totalVenta
                    );
                }
            }
        } catch (IOException e) {
            throw new ExportException("Error al generar CSV de ventas.", e);
        }
    }
}