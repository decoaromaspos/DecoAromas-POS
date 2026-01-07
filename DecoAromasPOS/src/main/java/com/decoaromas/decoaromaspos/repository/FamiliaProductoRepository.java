package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.familia.FamiliaCantidadProductosResponse;
import com.decoaromas.decoaromaspos.model.FamiliaProducto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamiliaProductoRepository extends JpaRepository<FamiliaProducto, Long> {

    List<FamiliaProducto> findByNombreIgnoreCase(String nombre);
    List<FamiliaProducto> findAllByIsDeletedIsFalse();

    // ----- Logica obtener Familias con atributo de cantidad de productos asociados y filtros
    @Query("""
        SELECT new com.decoaromas.decoaromaspos.dto.familia.FamiliaCantidadProductosResponse(
            f.familiaId, f.nombre, f.isDeleted, COUNT(p.productoId))
        FROM FamiliaProducto f
        LEFT JOIN Producto p ON p.familia = f
        WHERE (:nombre IS NULL OR LOWER(f.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
          AND (:isDeleted IS NULL OR f.isDeleted = :isDeleted)
        GROUP BY f.familiaId, f.nombre, f.isDeleted
        """)
    Page<FamiliaCantidadProductosResponse> findAllWithFiltersAndProductCount(
            @Param("nombre") String nombre,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);

}
