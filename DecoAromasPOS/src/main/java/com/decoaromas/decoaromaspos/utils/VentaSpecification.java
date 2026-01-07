package com.decoaromas.decoaromaspos.utils;

import com.decoaromas.decoaromaspos.dto.venta.VentaFilterDTO;
import com.decoaromas.decoaromaspos.enums.MedioPago;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.enums.TipoDocumento;
import com.decoaromas.decoaromaspos.model.PagoVenta;
import com.decoaromas.decoaromaspos.model.Venta;
import com.decoaromas.decoaromaspos.model.DetalleVenta;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class VentaSpecification {

    private VentaSpecification() {
        throw new IllegalStateException("Utility class");}

    public static Specification<Venta> conFiltros(ZonedDateTime fechaInicio, ZonedDateTime fechaFin, VentaFilterDTO filters) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            assert query != null;
            // Evitar duplicados al hacer join con tablas "OneToMany" (pagos o detalles)
            query.distinct(true);

            agregarFiltroRangoFechas(predicates, cb, root, fechaInicio, fechaFin);
            agregarFiltroTipoCliente(predicates, cb, root, filters.getTipoCliente());
            agregarFiltroMetodoPago(predicates, cb, root, filters.getMedioPago());
            agregarFiltroTipoDocumento(predicates, cb, root, filters.getTipoDocumento());
            agregarFiltroNumeroDocumentoParcial(predicates, cb, root, filters.getNumeroDocumentoParcial(), filters.getPendienteAsignacion());
            agregarFiltroRangoTotalNeto(predicates, cb, root, filters.getMinTotalNeto(), filters.getMaxTotalNeto());
            agregarFiltroUsuarioId(predicates, cb, root, filters.getUsuarioId());
            agregarFiltroClienteId(predicates, cb, root, filters.getClienteId());
            agregarFiltroProductoId(predicates, cb, root, filters.getProductoId());

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    // FILTRO POR TIPO (BOLETA o FACTURA)
    // Esto se aplica si el usuario seleccionó un tipo, independiente si está pendiente o emitido.
    private static void agregarFiltroTipoDocumento(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, TipoDocumento tipoDocumento) {
        if (tipoDocumento != null) {
            predicates.add(cb.equal(root.get("tipoDocumento"), tipoDocumento));
        }
    }

    // FILTRO POR NÚMERO PARCIAL (Búsqueda)
    private static void agregarFiltroNumeroDocumentoParcial(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, String numeroDocumentoParcial, Boolean pendienteAsignacion) {
        // Si el usuario está buscando un número, esto tiene prioridad y anula el filtro de "pendiente".
        if (StringUtils.hasText(numeroDocumentoParcial)) {
            String searchPattern = "%" + numeroDocumentoParcial.trim().toLowerCase() + "%";
            predicates.add(
                    cb.like(cb.lower(root.get("numeroDocumento")), searchPattern)
            );

            // NOTA: Una búsqueda por número implica `numeroDocumento IS NOT NULL`,
            // así que no necesitamos el filtro "pendienteAsignacion" si este existe.

        } else if (pendienteAsignacion != null) {
            // FILTRO POR ESTADO (Pendiente / Emitido). Se activa solo si NO se está buscando por número parcial.

            if (pendienteAsignacion) {
                // PENDIENTES
                predicates.add(cb.isNull(root.get("numeroDocumento")));
            } else {
                // EMITIDOS
                predicates.add(cb.isNotNull(root.get("numeroDocumento")));
            }
        }
    }

    private static void agregarFiltroMetodoPago(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, MedioPago medioPago) {
        if (medioPago != null) {
            // Hacemos un JOIN desde Venta (root) a su colección de pagos.
            Join<Venta, PagoVenta> pagoJoin = root.join("pagos");

            // Aplicamos el filtro sobre el atributo 'medioPago' de la entidad unida (PagoVenta)
            predicates.add(cb.equal(pagoJoin.get("medioPago"), medioPago));
        }
    }

    private static void agregarFiltroTipoCliente(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, TipoCliente tipoCliente) {
        if (tipoCliente != null) {
            predicates.add(cb.equal(root.get("tipoCliente"), tipoCliente));
        }
    }

    private static void agregarFiltroRangoTotalNeto(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, Double minTotalNeto, Double maxTotalNeto) {
        if (minTotalNeto != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("totalNeto"), minTotalNeto));
        }
        if (maxTotalNeto != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("totalNeto"), maxTotalNeto));
        }
    }

    private static void agregarFiltroRangoFechas(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, ZonedDateTime fechaInicio, ZonedDateTime fechaFin) {
        if (fechaInicio != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaInicio));
        }
        if (fechaFin != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaFin));
        }
    }

    private static void agregarFiltroUsuarioId(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, Long usuarioId) {
        if (usuarioId != null) {
            predicates.add(cb.equal(root.get("usuario").get("usuarioId"), usuarioId));
        }
    }

    private static void agregarFiltroProductoId(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, Long productoId) {
        if (productoId != null) {
            // Join desde Venta hacia la lista de 'detalles'
            Join<Venta, DetalleVenta> detallesJoin = root.join("detalles");

            // Comprar ID del producto dentro del detalle.
            predicates.add(cb.equal(detallesJoin.get("producto").get("productoId"), productoId));
        }
    }

    // 6. FILTRO POR CLIENTE (manejo de NULL)
    // - clienteId > 0: Buscar por ese clienteId específico.
    // - clienteId = 0 o clienteId = -1: Buscar ventas SIN cliente (clienteId IS NULL).
    // - clienteId = null: Ignorar el filtro (buscar con/sin cliente).
    private static void agregarFiltroClienteId(List<Predicate> predicates, CriteriaBuilder cb, Root<Venta> root, Long clienteId) {
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
