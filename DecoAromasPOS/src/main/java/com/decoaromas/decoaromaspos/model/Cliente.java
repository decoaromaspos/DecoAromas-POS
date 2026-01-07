package com.decoaromas.decoaromaspos.model;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
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
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clienteId;

    @Column(nullable = false)
    private String nombre;
    private String apellido;

    @Column(nullable = false, unique = true)
    private String rut;

    @Column(unique = true)
    private String correo;

    @Column(unique = true)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCliente tipo;

    private String ciudad;

    private Boolean activo = true;
}
