package com.decoaromas.decoaromaspos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "familia_producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamiliaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long familiaId;

    @Column(nullable = false, unique = true)
    private String nombre;

    private Boolean isDeleted = Boolean.FALSE;
}
