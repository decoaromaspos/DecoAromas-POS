package com.decoaromas.decoaromaspos.utils;

import com.decoaromas.decoaromaspos.dto.movimiento_inventario.MovimientoFilterDTO;
import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.enums.TipoMovimiento;
import com.decoaromas.decoaromaspos.model.MovimientoInventario;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimientoSpecification {

    private MovimientoSpecification() {
        throw new IllegalStateException("Utility class");
    }

    // Método estático para construir la Specification combinada
    public static Specification<MovimientoInventario> conFiltros(
            ZonedDateTime fechaInicio,
            ZonedDateTime fechaFin,
            MovimientoFilterDTO filters) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            agregarFiltroFechaInicio(predicates, cb, root, fechaInicio);
            agregarFiltroFechaFin(predicates, cb, root, fechaFin);
            agregarFiltroTipoMov(predicates, cb, root, filters.getTipo());
            agregarFiltroMotivoMov(predicates, cb, root, filters.getMotivo());
            agregarFiltroUsuarioId(predicates, cb, root, filters.getUsuarioId());
            agregarFiltroProductoId(predicates, cb, root, filters.getProductoId());

            return cb.and(predicates.toArray(new Predicate[0])); // Combina todos los predicados con un operador AND
        };
    }

    // Métodos helpers para filtrado

    private static void agregarFiltroUsuarioId(List<Predicate> predicates, CriteriaBuilder cb, Root<MovimientoInventario> root, Long usuarioId) {
        if (usuarioId != null) {
            // Navegación de la relación: root.get("usuario").get("usuarioId")
            predicates.add(cb.equal(root.get("usuario").get("usuarioId"), usuarioId));
        }
    }

    private static void agregarFiltroFechaInicio(List<Predicate> predicates, CriteriaBuilder cb, Root<MovimientoInventario> root, ZonedDateTime fechaInicio) {
        if (fechaInicio != null) {
            // gt (greater than) o ge (greater than or equal)
            predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaInicio));
        }
    }

    private static void agregarFiltroFechaFin(List<Predicate> predicates, CriteriaBuilder cb, Root<MovimientoInventario> root, ZonedDateTime fechaFin) {
        if (fechaFin != null) {
            // lt (less than) o le (less than or equal)
            predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaFin));
        }
    }

    private static void agregarFiltroTipoMov(List<Predicate> predicates, CriteriaBuilder cb, Root<MovimientoInventario>  root, TipoMovimiento tipo) {
        if (tipo != null) {
            predicates.add(cb.equal(root.get("tipo"), tipo));
        }
    }

    private static void agregarFiltroProductoId(List<Predicate> predicates, CriteriaBuilder cb, Root<MovimientoInventario> root, Long productoId) {
        if (productoId != null) {
            predicates.add(cb.equal(root.get("producto").get("productoId"), productoId));
        }
    }

    private static void agregarFiltroMotivoMov(List<Predicate> predicates, CriteriaBuilder cb, Root<MovimientoInventario> root, MotivoMovimiento motivo) {
        if (motivo != null) {
            predicates.add(cb.equal(root.get("motivo"), motivo));
        }
    }
}
