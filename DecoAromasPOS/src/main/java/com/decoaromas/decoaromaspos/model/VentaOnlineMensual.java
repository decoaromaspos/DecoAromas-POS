package com.decoaromas.decoaromaspos.model;

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
@Table(name = "venta_online_mensual",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "mes", "anio" }) })
public class VentaOnlineMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ventaOnlineMensualId;

    private ZonedDateTime fechaIngreso;
    private Integer mes;
    private Integer anio;
    private Double totalDetalle;
    private Double totalMayorista;
}
