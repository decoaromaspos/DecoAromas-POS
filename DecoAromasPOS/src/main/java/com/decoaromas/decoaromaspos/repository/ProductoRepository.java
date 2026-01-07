package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.dto.producto.ProductoAutoCompleteSelectProjection;
import com.decoaromas.decoaromaspos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {
    int countByAroma_AromaId(Long id);
    int countByFamilia_FamiliaId(Long familiaId);

    @Query("SELECT SUM(p.stock) FROM Producto p WHERE p.stock >= 0 AND p.activo = true")
    Optional<Double> sumStockActivosByStockGreaterThanZero();

    List<Producto> findByActivoTrue();

    List<Producto> findByActivoFalse();

    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    List<ProductoAutoCompleteSelectProjection> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Producto> findByNombreContainingIgnoreCaseAndActivo(String nombre, Boolean activo);

    Optional<Producto> findBySku(String sku);
    Optional<Producto> findBySkuIgnoreCase(String sku);

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    List<Producto> findByCodigoBarrasIsNull();

	List<Producto> findProductosByProductoIdIn(List<Long> productoIds);

    List<Producto> findByNombreIgnoreCaseAndActivoTrue(String nombre);

    List<Producto> findByNombreIgnoreCaseAndActivoTrueAndProductoIdNot(String nombre, Long productoId);
}
