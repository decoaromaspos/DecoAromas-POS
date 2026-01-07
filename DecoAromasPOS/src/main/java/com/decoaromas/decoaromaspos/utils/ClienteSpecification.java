package com.decoaromas.decoaromaspos.utils;

import com.decoaromas.decoaromaspos.dto.cliente.ClienteFilterDTO;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.Cliente;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ClienteSpecification {

    private ClienteSpecification() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<Cliente> conFiltros(ClienteFilterDTO filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtros de String Parcial (LIKE con ignorar mayúsculas/minúsculas)
            agregarFiltroNombreCompleto(predicates, criteriaBuilder, root, filters.getNombreCompletoParcial());
            agregarFiltrosRutParcial(predicates, criteriaBuilder, root, filters.getRutParcial());
            agregarFiltrosCorreoParcial(predicates, criteriaBuilder, root, filters.getCorreoParcial());
            agregarFiltrosTelefonoParcial(predicates, criteriaBuilder, root, filters.getTelefonoParcial());
            agregarFiltrosCiudadParcial(predicates, criteriaBuilder, root, filters.getCiudadParcial());

            //  Filtros de Coincidencia Exacta
            agregarFiltroTipoCliente(predicates, criteriaBuilder, root, filters.getTipo());
            agregarFiltroEstadoActivo(predicates, criteriaBuilder, root, filters.getActivo());

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Métodos helpers
    private static void agregarFiltroNombreCompleto(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String nombreCompleto) {
        if (StringUtils.hasText(nombreCompleto)) {
            String valorLimpio = "%" + nombreCompleto.trim().toLowerCase() + "%";

            // Coalesce: Si el apellido es null, usa un string vacío ""
            Expression<String> apellidoConCoalesce = cb.coalesce(root.get("apellido"), "");

            // Concatenamos nombre + " " + (apellido o "")
            Expression<String> nombreCompletoDb = cb.concat(
                    cb.concat(cb.lower(root.get("nombre")), " "),
                    cb.lower(apellidoConCoalesce)
            );

            // También buscar solo en el nombre por si el usuario no puso espacio
            Predicate searchInFull = cb.like(nombreCompletoDb, valorLimpio);
            Predicate searchInNombre = cb.like(cb.lower(root.get("nombre")), valorLimpio);

            predicates.add(cb.or(searchInFull, searchInNombre));
        }
    }

    private static void agregarFiltrosRutParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String rut) {
        if (StringUtils.hasText(rut)) {
            String searchPattern = "%" + rut.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("rut")), searchPattern));
        }
    }

    private static void agregarFiltrosCorreoParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String correo) {
        if (StringUtils.hasText(correo)) {
            String searchPattern = "%" + correo.trim().toLowerCase() + "%";
            predicates.add(
                    cb.like(cb.lower(root.get("correo")), searchPattern)
            );
        }
    }

    private static void agregarFiltrosTelefonoParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String telefono) {
        if (StringUtils.hasText(telefono)) {
            String searchPattern = "%" + telefono.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("telefono")), searchPattern));
        }
    }

    private static void agregarFiltrosCiudadParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, String ciudad) {
        if (StringUtils.hasText(ciudad)) {
            String searchPattern = "%" + ciudad.trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("ciudad")), searchPattern));
        }
    }

    private static void agregarFiltroTipoCliente(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, TipoCliente tipoCliente) {
        if (tipoCliente != null) {
            predicates.add(cb.equal(root.get("tipo"), tipoCliente));
        }
    }

    private static void agregarFiltroEstadoActivo(List<Predicate> predicates, CriteriaBuilder cb, Root<Cliente> root, Boolean activo) {
        if (activo != null) {
            predicates.add(cb.equal(root.get("activo"), activo));
        }
    }
}
