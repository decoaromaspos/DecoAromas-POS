package com.decoaromas.decoaromaspos.utils;

import com.decoaromas.decoaromaspos.dto.usuario.UsuarioFilterDTO;
import com.decoaromas.decoaromaspos.enums.Rol;
import com.decoaromas.decoaromaspos.model.Usuario;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UsuarioSpecification {

    private UsuarioSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Usuario> conFiltros(UsuarioFilterDTO filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- REGLA: Excluir siempre a los SUPER_ADMIN ---
            predicates.add(criteriaBuilder.notEqual(root.get("rol"), Rol.SUPER_ADMIN));

            // Filtros de String Parcial
            agregarFiltrosNombreCompleto(predicates, criteriaBuilder, root, filters.getNombreCompletoParcial());
            agregarFiltrosCorreoParcial(predicates, criteriaBuilder, root, filters.getCorreoParcial());
            agregarFiltrosUsernameParcial(predicates, criteriaBuilder, root, filters.getUsernameParcial());

            // Filtros de Coincidencia Exacta
            agregarFiltrosRol(predicates, criteriaBuilder, root, filters.getRol());
            agregarFiltrosEstadoActivo(predicates, criteriaBuilder, root, filters.getActivo());

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


    // Métodos helpers

    private static void agregarFiltrosRol(List<Predicate> predicates, CriteriaBuilder cb, Root<Usuario> root, Rol rol) {
        if (rol != null) {
            predicates.add(cb.equal(root.get("rol"), rol));
        }
    }

    private static void agregarFiltrosEstadoActivo(List<Predicate> predicates, CriteriaBuilder cb, Root<Usuario> root, Boolean activo) {
        if (activo != null) {
            predicates.add(cb.equal(root.get("activo"), activo));
        }
    }

    private static void agregarFiltrosUsernameParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Usuario> root, String username) {
        if (StringUtils.hasText(username)) {
            String usernameLimpio = username.trim().toLowerCase();
            String searchPattern = "%" + usernameLimpio + "%";
            predicates.add(cb.like(cb.lower(root.get("username")), searchPattern));
        }
    }

    private static void agregarFiltrosCorreoParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Usuario> root, String correo) {
        if (StringUtils.hasText(correo)) {
            String correoLimpio = correo.trim().toLowerCase();
            String searchPattern = "%" + correoLimpio + "%";
            predicates.add(cb.like(cb.lower(root.get("correo")), searchPattern));
        }
    }

    private static void agregarFiltrosNombreCompleto(List<Predicate> predicates, CriteriaBuilder cb, Root<Usuario> root, String nombreCompleto) {
        if (StringUtils.hasText(nombreCompleto)) {
            String nombreCompletoLimpio = nombreCompleto.trim().toLowerCase();
            String searchPattern = "%" + nombreCompletoLimpio + "%";

            // 1. Pasamos nombre y apellido a minúsculas
            Expression<String> nombreLower = cb.lower(root.get("nombre"));
            Expression<String> apellidoLower = cb.lower(root.get("apellido"));

            // 2. Concatenamos: (nombre + " ") + apellido
            Expression<String> nombreMasEspacio = cb.concat(nombreLower, " ");
            Expression<String> nombreCompletoDb = cb.concat(nombreMasEspacio, apellidoLower);

            // 3. Aplicamos el LIKE sobre la concatenación
            predicates.add(cb.like(nombreCompletoDb, searchPattern));
        }
    }

}
