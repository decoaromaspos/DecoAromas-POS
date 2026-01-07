package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.aroma.AromaCantidadProductosResponse;
import com.decoaromas.decoaromaspos.model.Aroma;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AromaRepository extends JpaRepository<Aroma, Long> {

    List<Aroma> findByNombreContainingIgnoreCase(String nombre);
    List<Aroma> findByNombreIgnoreCase(String nombre);
    List<Aroma> findAllByIsDeletedIsFalse();


    // ----- Logica obtener Aromas con atributo de cantidad de productos asociados y filtros asociados
    @Query("""
        SELECT new com.decoaromas.decoaromaspos.dto.aroma.AromaCantidadProductosResponse(
            a.aromaId, a.nombre, a.isDeleted, COUNT(p.productoId))
        FROM Aroma a
        LEFT JOIN Producto p ON p.aroma = a
        WHERE (:nombre IS NULL OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
          AND (:isDeleted IS NULL OR a.isDeleted = :isDeleted)
        GROUP BY a.aromaId, a.nombre, a.isDeleted
        """)
    Page<AromaCantidadProductosResponse> findAllWithFiltersAndProductCount(
            @Param("nombre") String nombre,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);
}
