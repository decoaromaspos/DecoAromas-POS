package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.model.Producto;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BarcodeExportService {

	private final ProductoRepository productoRepository;

	// Main generator with customizable size in cm
	private byte[] generateProductBarcodesPdf(List<Producto> productos, float barcodeWidthCm, float barcodeHeightCm) throws IOException {
		try (PDDocument document = new PDDocument()) {

			// Convert cm to points (PDF units)
			float cmToPoints = 72f / 2.54f;
			float barcodeWidth = barcodeWidthCm * cmToPoints;
			float barcodeHeight = barcodeHeightCm * cmToPoints;

			// Bigger margins and spacing
			float marginX = 30;   // left/right margin
			float marginY = 30;   // top/bottom margin
			float spacingX = 25;  // horizontal spacing between barcodes
			float spacingY = 25;  // vertical spacing between barcodes

			PDPage page = new PDPage(PDRectangle.LETTER); // Letter size
			document.addPage(page);

			PDPageContentStream contentStream = new PDPageContentStream(document, page);
			float pageWidth = page.getMediaBox().getWidth();
			float pageHeight = page.getMediaBox().getHeight();
			float startY = pageHeight - marginY;

			// Calculate how many barcodes fit per row/column with spacing
			int columns = (int) ((pageWidth - 2 * marginX + spacingX) / (barcodeWidth + spacingX));
			int rows = (int) ((pageHeight - 2 * marginY + spacingY) / (barcodeHeight + spacingY + 40)); // +40 for text

			if (columns < 1) columns = 1;
			if (rows < 1) rows = 1;

			int count = 0;

			for (Producto p : productos) {
				if (p.getCodigoBarras() == null || p.getCodigoBarras().isBlank()) continue;

				String code = p.getCodigoBarras().trim();
				if (code.length() == 12) code = "0" + code;
				if (code.length() != 13) continue;

				BufferedImage barcodeImage;
				try {
					barcodeImage = generateEAN13BarcodeImage(code, (int) barcodeWidth, (int) barcodeHeight);
				} catch (Exception e) {
					continue; // skip invalid barcodes
				}

				int col = count % columns;
				int row = (count / columns) % rows;

				float x = marginX + col * (barcodeWidth + spacingX);
				float y = startY - row * (barcodeHeight + spacingY + 40);

				// Add new page when full
				if (count > 0 && count % (columns * rows) == 0) {
					contentStream.close();
					page = new PDPage(PDRectangle.LETTER);
					document.addPage(page);
					contentStream = new PDPageContentStream(document, page);
					y = startY;
				}

				// Draw barcode
				PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, toBytes(barcodeImage), code);
				contentStream.drawImage(pdImage, x, y - barcodeHeight, barcodeWidth, barcodeHeight);

				// Draw numeric code
				contentStream.beginText();
				contentStream.setFont(PDType1Font.HELVETICA_BOLD, 8);
				float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(code) / 1000 * 8;
				contentStream.newLineAtOffset(x + (barcodeWidth - textWidth) / 2, y - barcodeHeight - 15);
				contentStream.showText(code);
				contentStream.endText();

				// Draw product name
				contentStream.beginText();
				contentStream.setFont(PDType1Font.HELVETICA, 9);
				textWidth = PDType1Font.HELVETICA.getStringWidth(p.getNombre()) / 1000 * 9;
				contentStream.newLineAtOffset(x + (barcodeWidth - textWidth) / 2, y - barcodeHeight - 30);
				contentStream.showText(p.getNombre());
				contentStream.endText();

				count++;
			}

			contentStream.close();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			document.save(baos);
			return baos.toByteArray();
		}
	}

	// ZXing barcode generator with custom width/height
	private BufferedImage generateEAN13BarcodeImage(String code, int width, int height) throws Exception {
		BitMatrix bitMatrix = new MultiFormatWriter().encode(
				code, BarcodeFormat.EAN_13, width, height);
		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}

	private byte[] toBytes(BufferedImage image) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		return baos.toByteArray();
	}

	// Public methods now accept width/height in cm
	public byte[] printAllBarcodes(float widthCm, float heightCm) throws IOException {
		List<Producto> productos = productoRepository.findAll();
		return generateProductBarcodesPdf(productos, widthCm, heightCm);
	}

	public byte[] printBarcodeList(List<Long> productIDs, float widthCm, float heightCm) throws IOException {
		// Get all unique products first
		List<Producto> uniqueProductos = productoRepository.findProductosByProductoIdIn(productIDs);

		// Create a map for a quick lookup by ID
		Map<Long, Producto> productoMap = uniqueProductos.stream()
				.collect(Collectors.toMap(Producto::getProductoId, p -> p));

		// Build a new list preserving order and duplicates
		List<Producto> repeatedProductos = new ArrayList<>();
		for (Long id : productIDs) {
			Producto p = productoMap.get(id);
			if (p != null) {
				repeatedProductos.add(p);
			}
		}

		return generateProductBarcodesPdf(repeatedProductos, widthCm, heightCm);
	}
}
