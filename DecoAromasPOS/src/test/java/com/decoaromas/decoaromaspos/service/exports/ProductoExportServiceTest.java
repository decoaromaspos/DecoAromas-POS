package com.decoaromas.decoaromaspos.service.exports;

import com.decoaromas.decoaromaspos.model.Aroma;
import com.decoaromas.decoaromaspos.model.FamiliaProducto;
import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoExportServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoExportService productoExportService;

    @Test
    void escribirProductosACsv_DeberiaManejarProductosConDatosNulos() {
        // Arrange
        Producto p1 = new Producto();
        p1.setProductoId(1L);
        p1.setNombre("Producto Test");
        p1.setActivo(true);
        // Dejamos familia, aroma y descripción nulos para probar los "N/A"

        Producto p2 = new Producto();
        p2.setProductoId(2L);
        p2.setNombre("Producto Completo");
        p2.setActivo(false);
        p2.setDescripcion("Una descripción");

        FamiliaProducto f = new FamiliaProducto(); f.setNombre("Familia 1");
        p2.setFamilia(f);

        Aroma a = new Aroma(); a.setNombre("Aroma 1");
        p2.setAroma(a);

        when(productoRepository.findAll(any(Specification.class))).thenReturn(List.of(p1, p2));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Act
        productoExportService.escribirProductosACsv(pw, null, null, null);

        // Assert
        String res = sw.toString();
        assertTrue(res.contains("Producto Test"));
        assertTrue(res.contains("N/A"), "Debe contener N/A para los campos nulos");
        assertTrue(res.contains("Activo"));
        assertTrue(res.contains("Inactivo"));
        assertTrue(res.contains("Aroma 1"));
    }
}