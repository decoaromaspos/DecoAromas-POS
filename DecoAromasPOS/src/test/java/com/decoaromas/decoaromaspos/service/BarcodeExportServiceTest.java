package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarcodeExportServiceTest {

	@Mock
	private ProductoRepository productoRepository;
	@InjectMocks
	private BarcodeExportService barcodeExportService;
	private Producto productoValido;
	private Producto productoInvalido;

	// Default test sizes in cm
	private final float widthCm = 3.5f;
	private final float heightCm = 2.0f;

	@BeforeEach
	void setUp() {
		productoValido = new Producto();
		productoValido.setProductoId(1L);
		productoValido.setNombre("Vela Aromática");
		productoValido.setCodigoBarras("1234567890128"); // EAN13 válido

		productoInvalido = new Producto();
		productoInvalido.setProductoId(2L);
		productoInvalido.setNombre("Difusor");
		productoInvalido.setCodigoBarras("ABC123"); // inválido
	}

	@Test
	@DisplayName("Test para imprimir todos los códigos de barra, genera un PDF con productos válidos")
	void testPrintAllBarcodes_GeneraPdfCorrectamente() throws IOException {
		when(productoRepository.findAll()).thenReturn(List.of(productoValido));

		byte[] pdfBytes = barcodeExportService.printAllBarcodes(widthCm, heightCm);

		assertNotNull(pdfBytes);
		assertTrue(pdfBytes.length > 100, "El PDF generado no debe estar vacío");
		verify(productoRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("Test para imprimir todos los códigos de barra, ignora productos sin código")
	void testPrintAllBarcodes_IgnoraProductoSinCodigo() throws IOException {
		productoValido.setCodigoBarras(null);
		when(productoRepository.findAll()).thenReturn(List.of(productoValido));

		byte[] pdfBytes = barcodeExportService.printAllBarcodes(widthCm, heightCm);

		assertNotNull(pdfBytes);
		assertTrue(pdfBytes.length > 0, "El PDF se genera aunque no haya códigos válidos");
		verify(productoRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("Test para imprimir lista de códigos de barra, genera un PDF en el orden listado")
	void testPrintBarcodeList_GeneraPdfEnOrden() throws IOException {
		when(productoRepository.findProductosByProductoIdIn(List.of(1L, 1L, 2L)))
				.thenReturn(List.of(productoValido, productoInvalido));

		byte[] pdfBytes = barcodeExportService.printBarcodeList(List.of(1L, 1L, 2L), widthCm, heightCm);

		assertNotNull(pdfBytes);
		assertTrue(pdfBytes.length > 100, "Debe generar un PDF válido");
		verify(productoRepository, times(1))
				.findProductosByProductoIdIn(List.of(1L, 1L, 2L));
	}

	@Test
	@DisplayName("Test para imprimir lista de códigos de barra, lista vacía")
	void testPrintBarcodeList_ListaVacia() throws IOException {
		when(productoRepository.findProductosByProductoIdIn(List.of()))
				.thenReturn(List.of());

		byte[] pdfBytes = barcodeExportService.printBarcodeList(List.of(), widthCm, heightCm);

		assertNotNull(pdfBytes);
		assertTrue(pdfBytes.length > 0, "Debe devolver PDF vacío pero válido");
	}

	@Test
	@DisplayName("Test para imprimir todos los códigos de barra, maneja códigos inválidos sin lanzar excepción")
	void testPrintAllBarcodes_CodigoInvalidoNoLanzaError() {
		when(productoRepository.findAll()).thenReturn(List.of(productoInvalido));

		assertDoesNotThrow(() -> barcodeExportService.printAllBarcodes(widthCm, heightCm),
				"No debe lanzar excepción con código de barras inválido");
	}
}
