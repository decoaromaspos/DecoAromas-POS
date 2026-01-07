package com.decoaromas.decoaromaspos.service.exports;

import com.decoaromas.decoaromaspos.exception.ExportException;
import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import com.decoaromas.decoaromaspos.utils.ProductoSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoExportService {

    private final ProductoRepository productoRepository;

    public void escribirProductosACsv(PrintWriter writer, Long aromaId, Long familiaId, Boolean activo) {
        writer.write('\ufeff');

        // Usar Specification existente para filtrado
        Specification<Producto> spec = ProductoSpecification.conFiltros(
                aromaId, familiaId, activo, null, null, null
        );

        List<Producto> productos = productoRepository.findAll(spec);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "SKU", "Cod. Barras", "Nombre", "Familia", "Aroma", "Stock", "Precio Detalle", "Precio Mayorista", "Costo", "Estado", "Descripción")
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {

            for (Producto p : productos) {
                csvPrinter.printRecord(
                        p.getProductoId(),
                        p.getSku(),
                        p.getCodigoBarras() != null ? p.getCodigoBarras() : "N/A",
                        p.getNombre(),
                        p.getFamilia() != null ? p.getFamilia().getNombre() : "N/A",
                        p.getAroma() != null ? p.getAroma().getNombre() : "N/A",
                        p.getStock(),
                        p.getPrecioDetalle(),
                        p.getPrecioMayorista(),
                        p.getCosto(),
                        Boolean.TRUE.equals(p.getActivo()) ? "Activo" : "Inactivo",
                        p.getDescripcion() != null ? p.getDescripcion() : "N/A"
                );
            }
        } catch (IOException e) {
            throw new ExportException("Error al generar el CSV de productos", e);
        }
    }
}