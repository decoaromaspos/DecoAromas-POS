package com.decoaromas.decoaromaspos.service;


import com.decoaromas.decoaromaspos.model.Configuracion;
import com.decoaromas.decoaromaspos.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    // --- Métodos específicos para obtener valores ---

    /**
     * Obtiene el valor de la meta mensual como un BigDecimal.
     *
     * @param valorPorDefecto El valor a devolver si la clave no se encuentra.
     * @return El valor de la meta mensual.
     */
    public BigDecimal getMetaMensual(BigDecimal valorPorDefecto) {
        return getConfiguracionAsBigDecimal("META_MENSUAL", valorPorDefecto);
    }

    /**
     * Obtiene la IP de la impresora como un String.
     *
     * @param valorPorDefecto El valor a devolver si la clave no se encuentra.
     * @return La IP de la impresora.
     */
    public String getIpImpresora(String valorPorDefecto) {
        return getConfiguracionAsString("IP_IMPRESORA", valorPorDefecto);
    }



    // --- Métodos genéricos y privados para la lógica de conversión ---

    public String getConfiguracionAsString(String clave, String valorPorDefecto) {
        return configuracionRepository.findByClave(clave)
                .map(Configuracion::getValor) // Extrae el valor si la configuración existe
                .orElse(valorPorDefecto);     // Devuelve el valor por defecto si no existe
    }

    public BigDecimal getConfiguracionAsBigDecimal(String clave, BigDecimal valorPorDefecto) {
        String valorString = getConfiguracionAsString(clave, null);
        if (valorString == null) {
            return valorPorDefecto;
        }
        try {
            return new BigDecimal(valorString);
        } catch (NumberFormatException e) {
            return valorPorDefecto;
        }
    }

    /**
     * Guarda o actualiza una configuración, asegurando que el valor esté limpio (trim).
     * @param clave La clave de la configuración.
     * @param valor El nuevo valor (se le aplicará trim si no es nulo).
     * @return La entidad de configuración guardada.
     */
    public Configuracion guardarConfiguracion(String clave, String valor) {
        String cleanedValor = (valor != null) ? valor.trim() : null; // Limpiar el valor antes de hacer nada con él.
        Configuracion config = configuracionRepository.findByClave(clave)
                .orElse(new Configuracion(clave, valor, "")); // Crea una nueva si no existe
        config.setValor(cleanedValor);
        return configuracionRepository.save(config);
    }
}