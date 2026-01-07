package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.familia.FamiliaCantidadProductosResponse;
import com.decoaromas.decoaromaspos.dto.familia.FamiliaRequest;
import com.decoaromas.decoaromaspos.dto.familia.FamiliaResponse;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.FamiliaProductoMapper;
import com.decoaromas.decoaromaspos.model.FamiliaProducto;
import com.decoaromas.decoaromaspos.repository.FamiliaProductoRepository;
import com.decoaromas.decoaromaspos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FamiliaProductoService {

    private final FamiliaProductoRepository familiaProductoRepository;
    private final ProductoRepository productoRepository;
    private final FamiliaProductoMapper familiaMapper;

    /**
     * Obtiene una lista de todas las familias, incluidas las eliminadas lógicamente (soft delete).
     * @return Lista de FamiliaResponse.
     */
    @Transactional(readOnly = true)
    public List<FamiliaResponse> listarFamiliasProductos() {
        return mapToList(familiaProductoRepository.findAll());
    }

    /**
     * Obtiene una lista de todas las familias activas (no eliminadas lógicamente).
     * @return Lista de FamiliaResponse.
     */
    @Transactional(readOnly = true)
    public List<FamiliaResponse> listarFamiliasActivos() {
        return mapToList(familiaProductoRepository.findAllByIsDeletedIsFalse());
    }

    /**
     * Crea una nueva familia de producto.
     * @param familiaRequest DTO con el nombre de la familia.
     * @return FamiliaResponse de la nueva familia creada.
     * @throws ExistsRegisterException si el nombre ya está en uso.
     */
    public FamiliaResponse crearFamilia(FamiliaRequest familiaRequest) {
        String cleanedNombre = familiaRequest.getNombre().trim();

        validarNombreUnico(cleanedNombre);

        FamiliaProducto familiaProducto = FamiliaProducto.builder()
                .nombre(cleanedNombre)
                .isDeleted(Boolean.FALSE)
                .build();

        familiaProductoRepository.save(familiaProducto);
        return familiaMapper.toResponse(familiaProducto);
    }

    /**
     * Actualiza el nombre de una familia existente.
     * @param id             ID de la familia a actualizar.
     * @param familiaRequest DTO con el nuevo nombre.
     * @return FamiliaResponse actualizada.
     * @throws ExistsRegisterException si el nuevo nombre ya está en uso por OTRA familia.
     */
    public FamiliaResponse actualizarFamiliaProducto(Long id, FamiliaRequest familiaRequest) {
        String cleanedNombre = familiaRequest.getNombre().trim();
        FamiliaProducto existente = obtenerFamiliaRealPorId(id);

        if (!existente.getNombre().equalsIgnoreCase(cleanedNombre)) {
            validarNombreUnico(cleanedNombre);
        }

        existente.setNombre(cleanedNombre);
        familiaProductoRepository.save(existente);
        return familiaMapper.toResponse(existente);
    }

    /**
     * Realiza un borrado lógico o reactivación de una familia.
     * @param id          ID de la familia.
     * @param nuevoEstado true para "eliminar" (ocultar), false para "reactivar".
     * @return FamiliaResponse actualizada.
     */
    public FamiliaResponse cambiarEstadoEliminadoFamiliaProducto(Long id, Boolean nuevoEstado) {
        FamiliaProducto existente = obtenerFamiliaRealPorId(id);
        existente.setIsDeleted(nuevoEstado);
        familiaProductoRepository.save(existente);

        return familiaMapper.toResponse(existente);
    }

    /**
     * Elimina una familia físicamente de la base de datos.
     * Falla si hay productos asociados a esta familia.
     * @param id ID de la familia a eliminar.
     * @throws DataIntegrityViolationException si hay productos asociados.
     */
    public void eliminarFamiliaProducto(Long id) {
        // Validación de integridad
        int conteo = productoRepository.countByFamilia_FamiliaId(id);

        if (conteo > 0) {
            throw new DataIntegrityViolationException("No se puede eliminar la familia. Tiene " + conteo + " productos asociados.");
        }

        FamiliaProducto familia = obtenerFamiliaRealPorId(id);
        familiaProductoRepository.delete(familia);
    }



    /**
     * Obtiene familias paginadas y filtradas, incluyendo el conteo de productos asociados.
     * @param page      Número de página.
     * @param size      Tamaño de página.
     * @param sortBy    Campo de ordenamiento.
     * @param nombre    Filtro por nombre (parcial).
     * @param isDeleted Filtro por estado de borrado lógico.
     * @return PaginacionResponse con los resultados.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<FamiliaCantidadProductosResponse> getFamiliasFiltradas(
            int page,
            int size,
            String sortBy,
            String nombre,
            Boolean isDeleted
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());

        Page<FamiliaCantidadProductosResponse> familiaPage =
                familiaProductoRepository.findAllWithFiltersAndProductCount(nombre, isDeleted, pageable);

        return PaginacionMapper.mapToResponse(familiaPage);
    }


    /**
     * Verifica si un nombre de familia (exacto) ya está en uso por una familia activa.
     * @param nombre Nombre a verificar.
     * @return AvailabilityResponse.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkNombreAvailability(String nombre) {
        String cleanedNombre = (nombre != null) ? nombre.trim() : "";
        if (cleanedNombre.isEmpty()) {
            return new AvailabilityResponse(false, "El nombre es obligatorio y no puede estar vacío.");
        }

        boolean existe = !familiaProductoRepository.findByNombreIgnoreCase(cleanedNombre).isEmpty();

        if (!existe) {
            return new AvailabilityResponse(true, "Nombre disponible.");
        } else {
            return new AvailabilityResponse(
                    false,
                    " El nombre exacto '" + nombre + "' está en uso por alguna familia (activa o inactiva). Ingrese otro.");
        }
    }


    // --- MÉTODOS INTERNOS / HELPERS ---

    /**
     *  Verifica si un nombre es único. En caso de no serlo, lanza excepción.
     * @param nombre String con nombre a validar unicidad.
     */
    private void validarNombreUnico(String nombre) {
        if (!familiaProductoRepository.findByNombreIgnoreCase(nombre).isEmpty()) {
            throw new ExistsRegisterException("Ya existe una familia (activa o inactiva) con nombre " + nombre + ". Ingrese otro.");
        }
    }

    /**
     * Obtiene la entidad FamiliaProducto por su ID.
     * Usado internamente para validaciones.
     * @param id ID de la familia.
     * @return La entidad FamiliaProducto.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    public FamiliaProducto obtenerFamiliaRealPorId(Long id) {
        return familiaProductoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe familia de producto con id " + id));
    }

    /**
     * Helper privado para mapear listas de FamiliaProducto a FamiliaResponse.
     * @param familias Lista de entidades.
     * @return Lista de DTOs.
     */
    private List<FamiliaResponse> mapToList(List<FamiliaProducto> familias) {
        return familias.stream()
                .map(familiaMapper::toResponse)
                .toList();
    }
}