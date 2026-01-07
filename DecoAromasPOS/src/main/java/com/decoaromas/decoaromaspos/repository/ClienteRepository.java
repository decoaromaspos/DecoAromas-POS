package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.reportes.ClienteAgregadoDTO;
import com.decoaromas.decoaromaspos.enums.TipoCliente;
import com.decoaromas.decoaromaspos.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

// En interfaces que extiendes de JPA, no es necesario usa @Repository, pero en las clases de implementacion si.
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    double countClienteByActivoTrue();
    Optional<Cliente> findByRutIgnoreCase(String rut);
    Optional<Cliente> findByCorreoIgnoreCase(String correo);
    List<Cliente> findByActivoTrue();
    List<Cliente> findByActivoFalse();

    // Buscar por nombre parcial (case insensitive)
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);

    // Buscar por nombre parcial y apellido parcial
    List<Cliente> findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase(String nombre, String apellido);

    // Verificaciones de unicidad
    boolean existsByRutIgnoreCase(String rut);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByTelefono(String telefono);

    // Verificaciones de unicidad excluyendo al Cliente con su id
    boolean existsByRutIgnoreCaseAndClienteIdNot(String rut, Long clienteId);
    boolean existsByCorreoIgnoreCaseAndClienteIdNot(String correo, Long clienteId);
    boolean existsByTelefonoAndClienteIdNot(String telefono, Long clienteId);



    // 1. Distribución de Clientes por Tipo
    @Query("SELECT new com.decoaromas.decoaromaspos.dto.reportes.ClienteAgregadoDTO(" +
            // MODIFICACIÓN CLAVE: Convertir el enum 'c.tipo' a String para que coincida con el constructor del DTO
            "    CAST(c.tipo AS string), " +
            "    CAST(COUNT(c.clienteId) AS double)) " +
            "FROM Cliente c " +
            "WHERE c.activo = true " +
            "GROUP BY c.tipo")
    List<ClienteAgregadoDTO> countClientesByTipo();


    // Nuevo método para contar clientes activos y opcionalmente por tipo.
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.activo = :activo AND (:tipoCliente IS NULL OR c.tipo = :tipoCliente)")
    long countByActivoAndTipoOpcional(@Param("activo") Boolean activo, @Param("tipoCliente") TipoCliente tipoCliente);

    // Nuevo método adaptado para la recencia, filtrando por tipoCliente
    @Query("SELECT c.clienteId FROM Cliente c WHERE c.activo = true " +
            "  AND (:tipoCliente IS NULL OR c.tipo = :tipoCliente) " + // Filtro de Tipo Cliente
            "  AND c.clienteId NOT IN (" +
            "    SELECT DISTINCT v.cliente.clienteId FROM Venta v WHERE v.fecha >= :fechaLimite AND v.cliente IS NOT NULL" +
            "  )")
    List<Long> findClientesActivosInactivosDesde(
            @Param("fechaLimite") ZonedDateTime fechaLimite,
            @Param("tipoCliente") TipoCliente tipoCliente
    );

    // Total de clientes activos
    long countByActivo(boolean activo);

    // Total de clientes activos por tipo
    long countByActivoAndTipo(boolean activo, TipoCliente tipo);

}
