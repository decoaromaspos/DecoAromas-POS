package com.decoaromas.decoaromaspos.utils;

import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.Cotizacion;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class CotizacionSpecification {

    // Constructor privado para ocultar el público implícito
    private CotizacionSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Cotizacion> conFiltros(
            ZonedDateTime fechaInicio,
            ZonedDateTime fechaFin,
            TipoCliente tipoCliente,
            Double minTotalNeto,
            Double maxTotalNeto,
            Long usuarioId,
            Long clienteId) { // valor especial (ej.: 0 o -1) para indicar "sin cliente"

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                query.distinct(true); // Evitar Ventas duplicados
            }

            // Delegar filtros a métodos privados para reducir complejidad
            agregarFiltrosFecha(predicates, cb, root, fechaInicio, fechaFin);
            agregarFiltroTipoCliente(predicates, cb, root, tipoCliente);
            agregarFiltrosMonto(predicates, cb, root, minTotalNeto, maxTotalNeto);
            agregarFiltroUsuario(predicates, cb, root, usuarioId);
            agregarFiltroCliente(predicates, cb, root, clienteId);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    // Métodos helpers para reducir la complejidad del método principal

    private static void agregarFiltrosFecha(List<Predicate> predicates, CriteriaBuilder cb, Root<Cotizacion> root, ZonedDateTime fechaInicio, ZonedDateTime fechaFin) {
        if (fechaInicio != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), fechaInicio));
        }
        if (fechaFin != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), fechaFin));
        }
    }

    private static void agregarFiltroTipoCliente(List<Predicate> predicates, CriteriaBuilder cb, Root<Cotizacion> root, TipoCliente tipoCliente) {
        if (tipoCliente != null) {
            predicates.add(cb.equal(root.get("tipoCliente"), tipoCliente));
        }
    }

    private static void agregarFiltrosMonto(List<Predicate> predicates, CriteriaBuilder cb, Root<Cotizacion> root, Double minTotalNeto, Double maxTotalNeto) {
        if (minTotalNeto != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("totalNeto"), minTotalNeto));
        }
        if (maxTotalNeto != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("totalNeto"), maxTotalNeto));
        }
    }

    private static void agregarFiltroUsuario(List<Predicate> predicates, CriteriaBuilder cb, Root<Cotizacion> root, Long usuarioId) {
        if (usuarioId != null) {
            predicates.add(cb.equal(root.get("usuario").get("usuarioId"), usuarioId));
        }
    }

    // FILTRO POR CLIENTE (manejo de NULL)
    // - clienteId > 0: Buscar por ese clienteId específico.
    // - clienteId =< 0: Buscar ventas SIN cliente (clienteId IS NULL).
    // - clienteId = null: Ignorar el filtro.
    private static void agregarFiltroCliente(List<Predicate> predicates, CriteriaBuilder cb, Root<Cotizacion> root, Long clienteId) {
        if (clienteId != null) {
            if (clienteId > 0) {
                predicates.add(cb.equal(root.get("cliente").get("id"), clienteId));
            } else {
                // Buscar ventas sin cliente (clienteId IS NULL)
                predicates.add(cb.isNull(root.get("cliente")));
            }
        }
    }
}
