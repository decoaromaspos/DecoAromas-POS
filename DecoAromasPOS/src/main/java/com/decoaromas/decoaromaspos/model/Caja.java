package com.decoaromas.decoaromaspos.model;

import com.decoaromas.decoaromaspos.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cajaId;

    private ZonedDateTime fechaApertura;
    private Double efectivoApertura;

    private ZonedDateTime fechaCierre;

    private Double efectivoCierre;
    private Double mercadoPagoCierre;
    private Double bciCierre;
    private Double botonDePagoCierre;
    private Double transferenciaCierre;
    private Double postCierre;

    @Enumerated(EnumType.STRING)
    private EstadoCaja estado;
    private Double diferenciaReal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuarioId")
    private Usuario usuario;
}
