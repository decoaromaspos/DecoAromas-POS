package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion,Long>, JpaSpecificationExecutor<Cotizacion> {

}
