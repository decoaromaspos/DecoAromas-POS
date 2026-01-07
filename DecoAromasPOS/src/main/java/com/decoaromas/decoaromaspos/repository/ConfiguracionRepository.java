package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {

    /**
     * Busca una configuración por su clave.
     * Usamos Optional para manejar de forma segura el caso de que la clave no exista.
     *
     * @param clave La clave única de la configuración a buscar.
     * @return Un Optional que contiene la configuración si se encuentra, o vacío si no.
     */
    Optional<Configuracion> findByClave(String clave);
}