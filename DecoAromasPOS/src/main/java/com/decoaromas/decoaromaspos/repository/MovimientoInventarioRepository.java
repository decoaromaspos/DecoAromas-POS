package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.enums.MotivoMovimiento;
import com.decoaromas.decoaromaspos.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario,Long>, JpaSpecificationExecutor<MovimientoInventario> {
    List<MovimientoInventario> findByProducto_ProductoId(Long id);
    List<MovimientoInventario> findByFechaBetween(ZonedDateTime startOfDay, ZonedDateTime endOfDay);
    List<MovimientoInventario> findByUsuario_UsuarioId(Long id);
    List<MovimientoInventario> findByMotivo(MotivoMovimiento motivo);
}
