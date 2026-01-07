package com.decoaromas.decoaromaspos.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarcodeServiceTest {

    private BarcodeService barcodeService;

    @BeforeEach
    void setUp() {
        barcodeService = new BarcodeService();
    }

    @Test
    @DisplayName("Test generar EAN, debe generar codigo correcto")
    void generarEAN13_deberiaGenerarCodigoCorrecto() {
        Long id = 12345L;
        String codigo = barcodeService.generarEAN13(id);

        // Debe tener 13 caracteres
        assertEquals(13, codigo.length());

        // Debe empezar con "200"
        assertTrue(codigo.startsWith("200"));

        // Los 9 dígitos siguientes corresponden al id formateado
        String expectedMiddle = String.format("%09d", id);
        assertEquals("200" + expectedMiddle, codigo.substring(0, 12));

        // Validar que el dígito verificador sea correcto
        int checkDigitCalculado = barcodeService.calcularCheckDigit(codigo.substring(0, 12));
        int checkDigitCodigo = Character.getNumericValue(codigo.charAt(12));
        assertEquals(checkDigitCalculado, checkDigitCodigo);
    }

    @Test
    @DisplayName("Test calcular y verificar digito con base12, debe generar digito correcto")
    void calcularCheckDigit_conBase12Valida_deberiaRetornarDigitoCorrecto() {
        // Ejemplo con un número de 12 dígitos (puedes usar uno conocido)
        String base12 = "200000012345";
        int checkDigit = barcodeService.calcularCheckDigit(base12);

        // El valor esperado (calculado manualmente o con otra herramienta)
        // Aquí lo calculamos con la misma lógica para que coincida
        int sum = 0;
        for (int i = 0; i < base12.length(); i++) {
            int digit = Character.getNumericValue(base12.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int mod = sum % 10;
        int esperado = (mod == 0) ? 0 : 10 - mod;

        assertEquals(esperado, checkDigit);
    }

    @Test
    @DisplayName("Test calcular y verificar digito con base12 invalida, debe generar excepcion")
    void calcularCheckDigit_conBase12Invalida_deberiaLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> {
            barcodeService.calcularCheckDigit("123"); // menos de 12 dígitos
        });

        assertThrows(IllegalArgumentException.class, () -> {
            barcodeService.calcularCheckDigit(null); // nulo
        });

        assertThrows(IllegalArgumentException.class, () -> {
            barcodeService.calcularCheckDigit("1234567890123"); // más de 12 dígitos
        });
    }
}
