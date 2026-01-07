package com.decoaromas.decoaromaspos.utils;

import com.decoaromas.decoaromaspos.model.Producto;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class ProductoSpecification {

    private ProductoSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Producto> conFiltros(
            Long aromaId,
            Long familiaId,
            Boolean activo,
            String nombre,
            String sku,
            String codigoBarras
    ) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            agregarFiltroAroma(predicates, cb, root, aromaId);
            agregarFiltroFamilia(predicates, cb, root, familiaId);
            agregarFiltroEstadoActivo(predicates, cb, root, activo);
            agregarFiltroNombreParcial(predicates, cb, root, nombre);
            agregarFiltroSKUParcial(predicates, cb, root, sku);
            agregarFiltroCodigoBarras(predicates, cb, root, codigoBarras);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    public static Specification<Producto> bajoStockconFiltros(
            Long aromaId,
            Long familiaId,
            String nombre,
            Integer umbralMaximo
    ) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            agregarFiltroEstadoActivo(predicates, cb, root, true);

            agregarFiltroAroma(predicates, cb, root, aromaId);
            agregarFiltroFamilia(predicates, cb, root, familiaId);
            agregarFiltroNombreParcial(predicates, cb, root, nombre);
            agregarFiltroUmbralMaxStock(predicates, cb, root, umbralMaximo);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }



    public static Specification<Producto> fueraStockconFiltros(
            Long aromaId,
            Long familiaId,
            String nombre
    ) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            agregarFiltroEstadoActivo(predicates, cb, root, true);
            predicates.add(cb.lessThanOrEqualTo(root.get("stock"), 0));

            agregarFiltroAroma(predicates, cb, root, aromaId);
            agregarFiltroFamilia(predicates, cb, root, familiaId);
            agregarFiltroNombreParcial(predicates, cb, root, nombre);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Métodos helpers

    // FILTRO POR UMBRAL DE STOCK
    private static void agregarFiltroUmbralMaxStock(List<Predicate> predicates, CriteriaBuilder cb, Root<Producto> root, Integer umbralMaximo) {
        if (umbralMaximo != null) {
            predicates.add(cb.between(root.get("stock"), 1, umbralMaximo));
        }
    }

    // FILTRO POR CÓDIGO DE BARRAS (Suele ser búsqueda exacta, pero se usa LIKE por si acaso)
    private static void agregarFiltroCodigoBarras(List<Predicate> predicates, CriteriaBuilder cb, Root<Producto> root, String codigoBarras) {
        if (codigoBarras != null && !codigoBarras.trim().isEmpty()) {
            String likePattern = "%" + codigoBarras + "%";
            predicates.add(cb.like(root.get("codigoBarras"), likePattern));
        }
    }

    // FILTRO POR NOMBRE (Búsqueda parcial, insensible a mayúsculas/minúsculas)
    private static void agregarFiltroNombreParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Producto> root, String nombre) {
        if (nombre != null && !nombre.trim().isEmpty()) {
            String likePattern = "%" + nombre.toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("nombre")), likePattern));
        }
    }

    // FILTRO POR SKU (Búsqueda parcial/exacta)
    private static void agregarFiltroSKUParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Producto> root, String sku) {
        if (sku != null && !sku.trim().isEmpty()) {
            String likePattern = "%" + sku.toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("sku")), likePattern));
        }
    }

    // FILTRO POR ID DE AROMA
    private static void agregarFiltroAroma(List<Predicate> predicates, CriteriaBuilder cb, Root<Producto> root, Long aromaId) {
        if (aromaId != null) {
            predicates.add(cb.equal(root.get("aroma").get("aromaId"), aromaId));
        }
    }

    // FILTRO POR ID DE FAMILIA
    private static void agregarFiltroFamilia(List<Predicate> predicates, CriteriaBuilder cb, Root<Producto> root, Long familiaId) {
        if (familiaId != null) {
            predicates.add(cb.equal(root.get("familia").get("familiaId"), familiaId));
        }
    }

    // FILTRO POR ESTADO ACTIVO DE PRODUCTO
    private static void agregarFiltroEstadoActivo(List<Predicate> predicates, CriteriaBuilder cb, Root<Producto> root, Boolean activo) {
        if (activo != null) {
            predicates.add(cb.equal(root.get("activo"), activo));
        }
    }

}