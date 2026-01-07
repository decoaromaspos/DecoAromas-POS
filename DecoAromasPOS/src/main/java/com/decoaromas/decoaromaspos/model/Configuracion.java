package com.decoaromas.decoaromaspos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una configuración genérica de la aplicación
 * en formato clave-valor. La clave debe ser única.
 */
@Entity
@Table(name = "configuracion")
@Data
@NoArgsConstructor
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave", unique = true, nullable = false, length = 100)
    private String clave;

    @Column(name = "valor", nullable = true, length = 500)
    private String valor;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    // Constructor útil para crear nuevas configuraciones
    public Configuracion(String clave, String valor, String descripcion) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }
}