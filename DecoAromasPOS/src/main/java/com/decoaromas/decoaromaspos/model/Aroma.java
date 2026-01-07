package com.decoaromas.decoaromaspos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aroma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aromaId;

    @Column(nullable = false, unique = true)
    private String nombre;

    private Boolean isDeleted = Boolean.FALSE;
}
