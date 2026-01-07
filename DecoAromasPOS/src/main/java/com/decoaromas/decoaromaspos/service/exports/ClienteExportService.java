package com.decoaromas.decoaromaspos.service.exports;

import com.decoaromas.decoaromaspos.exception.ExportException;
import com.decoaromas.decoaromaspos.model.Cliente;
import com.decoaromas.decoaromaspos.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteExportService {

    private final ClienteRepository clienteRepository;

    public void escribirClientesACsv(PrintWriter writer) {
        writer.write('\ufeff'); //  BOM para UTF-8
        List<Cliente> clientes = clienteRepository.findAll();

        // Solución al Deprecated: Usar Builder
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Nombre", "Apellido", "RUT", "Correo", "Teléfono", "Tipo", "Ciudad", "Estado")
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {

            for (Cliente cliente : clientes) {
                csvPrinter.printRecord(
                        cliente.getClienteId(),
                        cliente.getNombre(),
                        cliente.getApellido(),
                        cliente.getRut(),
                        cliente.getCorreo(),
                        cliente.getTelefono(),
                        cliente.getTipo(),
                        cliente.getCiudad(),
                        Boolean.TRUE.equals(cliente.getActivo()) ? "Activo" : "Inactivo"
                );
            }
        } catch (IOException e) {
            throw new ExportException("Error al generar el archivo CSV", e);
        }
    }
}