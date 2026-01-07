package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.response.UnauthorizedResponse;
import com.decoaromas.decoaromaspos.service.BarcodeExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.IS_AUTHENTICATED;

@RestController
@RequestMapping("/api/barcodes")
@RequiredArgsConstructor
@Tag(name = "Exportación de Códigos de Barras", description = "API para generar y descargar PDFs con etiquetas de códigos de barras (EAN-13).")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/barcodes\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class BarcodeExportController {

	private final BarcodeExportService barcodeExportService;

	@Operation(summary = "Exportar todos los códigos de barras", description = "Genera un PDF con los códigos de barras de TODOS los productos registrados. Omite productos sin código válido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generado correctamente",
                    content = @Content(mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary", description = "Archivo PDF binario"))),
            @ApiResponse(responseCode = "500", description = "Error al generar el documento PDF",
                    content = @Content(mediaType = "text/plain"))
    })
	@GetMapping("/exportAll")
	@PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<byte[]> exportBarcodesPdf(
            @Parameter(description = "Ancho de cada código de barras en cm", example = "3.0")
            @RequestParam(defaultValue = "3") float widthCm,

            @Parameter(description = "Alto de cada código de barras en cm", example = "1.2")
            @RequestParam(defaultValue = "1.2") float heightCm) throws Exception {

		byte[] pdfBytes = barcodeExportService.printAllBarcodes(widthCm, heightCm);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos_barcodes.pdf")
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdfBytes);
	}

    @Operation(summary = "Exportar códigos de barras de lista seleccionada", description = "Genera un PDF basado en una lista de IDs. Permite IDs duplicados para imprimir múltiples etiquetas del mismo producto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generado correctamente",
                    content = @Content(mediaType = "application/pdf",
                            schema = @Schema(type = "string", format = "binary", description = "Archivo PDF binario"))),
            @ApiResponse(responseCode = "400", description = "Lista de IDs vacía o inválida"),
            @ApiResponse(responseCode = "500", description = "Error al generar el documento PDF")
    })
	@PostMapping("/exportList")
	@PreAuthorize(IS_AUTHENTICATED)
	public ResponseEntity<byte[]> exportBarcodesList(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Lista de IDs de productos. Se respetan duplicados para impresión múltiple.",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(type = "integer", example = "10")),
                            examples = @ExampleObject(value = "[15, 15, 15, 20, 21]")
                    )
            )
            @RequestBody List<Long> productIds,

            @Parameter(description = "Ancho de cada código de barras en cm", example = "3.5")
            @RequestParam(defaultValue = "3") float widthCm,

            @Parameter(description = "Alto de cada código de barras en cm", example = "1.5")
            @RequestParam(defaultValue = "1.2") float heightCm) throws Exception {

		byte[] pdfBytes = barcodeExportService.printBarcodeList(productIds, widthCm, heightCm);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=productos_barcodes.pdf")
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdfBytes);
	}
}
