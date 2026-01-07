package com.decoaromas.decoaromaspos.service;

import org.springframework.stereotype.Service;

@Service
public class BarcodeService {

    /**
     * Genera un código EAN-13 válido usando el ID de un producto.
     * Ejemplo: prefijo 200 + id + ceros + check digit
     * @param id El ID único del producto.
     * @return El código EAN-13 completo.
     */
    public String generarEAN13(Long id) {
        // Prefijo reservado para uso interno (puede ser 200-299)
        String base = "200" + String.format("%09d", id); // 12 dígitos
        int checkDigit = calcularCheckDigit(base);
        return base + checkDigit;
    }

    /**
     * Calcula el dígito verificador EAN-13.
     * @param base12 Los primeros 12 dígitos del código.
     * @return El dígito verificador (check digit).
     */
    public int calcularCheckDigit(String base12) {
        if (base12 == null || base12.length() != 12) {
            throw new IllegalArgumentException("La base debe tener 12 dígitos.");
        }

        int sum = 0;
        for (int i = 0; i < base12.length(); i++) {
            int digit = Character.getNumericValue(base12.charAt(i));
            // posiciones impares (índice par 0, 2, 4...) se multiplican por 1
            // posiciones pares (índice impar 1, 3, 5...) se multiplican por 3
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int mod = sum % 10;
        return (mod == 0) ? 0 : 10 - mod;
    }
}