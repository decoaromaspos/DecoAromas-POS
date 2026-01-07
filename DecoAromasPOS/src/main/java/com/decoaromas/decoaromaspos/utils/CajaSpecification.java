package com.decoaromas.decoaromaspos.utils;

import com.decoaromas.decoaromaspos.enums.EstadoCaja;
import com.decoaromas.decoaromaspos.model.Caja;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class CajaSpecification {

    private CajaSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Caja> conFiltros(
        ZonedDateTime fechaInicio,
        ZonedDateTime fechaFin,
        EstadoCaja estado,
        Boolean cuadrada,
        Long usuarioId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            agregarFiltroRangoFechas(predicates, cb, root, fechaInicio, fechaFin);
            agregarFiltroEstadoCaja(predicates, cb, root, estado);
            agregarRiltroUsuarioId(predicates, cb, root, usuarioId);
            agregarFiltroEstadoCuadrada(predicates, cb, root, cuadrada);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // FILTRO POR RANGO DE FECHAS (Estrategia de Solapamiento)
    private static void agregarFiltroRangoFechas(List<Predicate> predicates, CriteriaBuilder cb, Root<Caja> root, ZonedDateTime fechaInicio, ZonedDateTime fechaFin) {
        // Se asume que la caja está activa durante el rango si:
        // (Fecha_Apertura <= Fecha_Fin_Filtro) Y (Fecha_Cierre O Fecha_Actual >= Fecha_Inicio_Filtro)
        if (fechaInicio != null && fechaFin != null) {
            // El inicio de la caja debe ser ANTES o IGUAL al fin del filtro
            Predicate inicioAnteriorAlFinFiltro = cb.lessThanOrEqualTo(root.get("fechaApertura"), fechaFin);

            // El fin de la caja debe ser DESPUÉS o IGUAL al inicio del filtro.
            Predicate finPosteriorAlInicioFiltro;

            // Si fechaCierre es NULL (caja ABIERTA), usamos la fecha actual o el fin del rango como límite.
            Predicate cajaCerradaYFinPosterior = cb.greaterThanOrEqualTo(root.get("fechaCierre"), fechaInicio);

            // Para cajas abiertas, asumimos que están activas hasta ahora, por lo que deben aparecer si el filtro
            // incluye el presente. Si el filtro de fechaInicio es anterior al presente, debe incluir cajas abiertas.
            Predicate cajaAbierta = cb.isNull(root.get("fechaCierre"));

            // Una caja debe aparecer si (ya cerró Y su fin es posterior al inicio del filtro)
            // O (aún está abierta Y la fecha de inicio del filtro no es futuro).
            finPosteriorAlInicioFiltro = cb.or(
                    cajaCerradaYFinPosterior,
                    cajaAbierta
            );

            predicates.add(cb.and(inicioAnteriorAlFinFiltro, finPosteriorAlInicioFiltro));
        }
    }

    private static void agregarFiltroEstadoCaja(List<Predicate> predicates, CriteriaBuilder cb, Root<Caja> root, EstadoCaja estadoCaja) {
        if (estadoCaja != null) {
            predicates.add(cb.equal(root.get("estado"), estadoCaja));
        }
    }

    private static void agregarRiltroUsuarioId(List<Predicate> predicates, CriteriaBuilder cb, Root<Caja> root, Long usuarioId) {
        if (usuarioId != null) {
            predicates.add(cb.equal(root.get("usuario").get("usuarioId"), usuarioId));
        }
    }

    private static void agregarFiltroEstadoCuadrada(List<Predicate> predicates, CriteriaBuilder cb, Root<Caja> root, Boolean cuadrada) {
        if (cuadrada != null) {
            if (cuadrada) {
                // Cuadrada: diferenciaReal = 0
                predicates.add(cb.equal(root.get("diferenciaReal"), 0.0));
            } else {
                // No Cuadrada: diferenciaReal != 0
                predicates.add(cb.notEqual(root.get("diferenciaReal"), 0.0));
            }
        }
    }


}
