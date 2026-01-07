package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.aroma.AromaCantidadProductosResponse;
import com.decoaromas.decoaromaspos.dto.aroma.AromaRequest;
import com.decoaromas.decoaromaspos.dto.aroma.AromaResponse;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.AromaMapper;
import com.decoaromas.decoaromaspos.model.Aroma;
import com.decoaromas.decoaromaspos.repository.AromaRepository;
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

/**
 * Servicio para la gestión de Aromas.
 * Se encarga del CRUD y la lógica de negocio de la entidad Aroma.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AromaService {

    private final AromaRepository aromaRepository;
    private final ProductoRepository productoRepository;
    private final AromaMapper aromaMapper;


    /**
     * Obtiene una lista de todos los aromas, incluidos los eliminados lógicamente (soft delete).
     * @return Lista de AromaResponse.
     */
    @Transactional(readOnly = true)
    public List<AromaResponse> listarAromas() {
        return mapToList(aromaRepository.findAll());
    }

    /**
     * Obtiene una lista de todos los aromas activos (no eliminados lógicamente).
     * @return Lista de AromaResponse.
     */
    @Transactional(readOnly = true)
    public List<AromaResponse> listarAromasActivos() {
        return mapToList(aromaRepository.findAllByIsDeletedIsFalse());
    }

    /**
     * Busca aromas cuyo nombre contenga parcialmente el texto provisto.
     * @param nombre Texto a buscar.
     * @return Lista de AromaResponse.
     */
    @Transactional(readOnly = true)
    public List<AromaResponse> buscarAromaPorNombreParcial(String nombre) {
        return mapToList(aromaRepository.findByNombreContainingIgnoreCase(nombre));
    }

    /**
     * Obtiene un aroma específico por su ID.
     * @param id El ID del aroma.
     * @return AromaResponse.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    @Transactional(readOnly = true)
    public AromaResponse obtenerAromaPorId(Long id) {
        Aroma existente = obtenerAromaRealPorId(id);
        return aromaMapper.toResponse(existente);
    }

    /**
     * Crea un nuevo aroma.
     * @param aromaRequest DTO con el nombre del aroma.
     * @return AromaResponse del nuevo aroma creado.
     * @throws ExistsRegisterException si el nombre ya está en uso.
     */
    public AromaResponse crearAroma(AromaRequest aromaRequest) {
        String cleanedNombre = aromaRequest.getNombre().trim();

        validarNombreUnico(cleanedNombre);

        Aroma aroma = Aroma.builder()
                .nombre(cleanedNombre)
                .isDeleted(Boolean.FALSE)
                .build();

        aromaRepository.save(aroma);
        return aromaMapper.toResponse(aroma);
    }

    /**
     * Actualiza el nombre de un aroma existente.
     * @param id           ID del aroma a actualizar.
     * @param aromaRequest DTO con el nuevo nombre.
     * @return AromaResponse actualizado.
     * @throws ExistsRegisterException si el nuevo nombre ya está en uso por OTRO aroma.
     */
    public AromaResponse actualizarAroma(Long id, AromaRequest aromaRequest) {
        String cleanedNombre = aromaRequest.getNombre().trim();
        Aroma existente = obtenerAromaRealPorId(id);

        // Si el nombre cambió, validar que el nuevo no esté en uso
        if (!existente.getNombre().equalsIgnoreCase(cleanedNombre)) {
            validarNombreUnico(cleanedNombre);
        }

        existente.setNombre(cleanedNombre);
        aromaRepository.save(existente);
        return aromaMapper.toResponse(existente);
    }

    /**
     * Realiza un borrado lógico o reactivación de un aroma.
     * @param id          ID del aroma.
     * @param nuevoEstado true para "eliminar" (ocultar), false para "reactivar".
     * @return AromaResponse actualizado.
     */
    public AromaResponse cambiarEstadoEliminadoAroma(Long id, Boolean nuevoEstado) {
        Aroma existente = obtenerAromaRealPorId(id);
        existente.setIsDeleted(nuevoEstado);
        aromaRepository.save(existente);

        return aromaMapper.toResponse(existente);
    }

    /**
     * Elimina un aroma físicamente de la base de datos.
     * Falla si hay productos asociados a este aroma.
     * @param id ID del aroma a eliminar.
     * @throws DataIntegrityViolationException si hay productos asociados.
     */
    public void eliminarAroma(Long id) {
        // Validación de integridad: Verifica si hay productos usando este aroma.
        // AromaService no debería depender de ProductoRepository,
        // esta es una validación de negocio necesaria para un mensaje de error.
        int conteo = productoRepository.countByAroma_AromaId(id);

        if (conteo > 0) {
            // Lanza una excepción específica que el ControllerAdvice pueda capturar.
            throw new DataIntegrityViolationException("No se puede eliminar el aroma. Tiene " + conteo + " productos asociados.");
        }

        Aroma aroma = obtenerAromaRealPorId(id);
        aromaRepository.delete(aroma);
    }

    /**
     * Obtiene aromas paginados y filtrados, incluyendo el conteo de productos asociados.
     * @param page      Número de página.
     * @param size      Tamaño de página.
     * @param sortBy    Campo de ordenamiento.
     * @param nombre    Filtro por nombre (parcial).
     * @param isDeleted Filtro por estado de borrado lógico.
     * @return PaginacionResponse con los resultados.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<AromaCantidadProductosResponse> getAromasFiltrados(
            int page,
            int size,
            String sortBy,
            String nombre,
            Boolean isDeleted
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());

        Page<AromaCantidadProductosResponse> aromasPage =
                aromaRepository.findAllWithFiltersAndProductCount(nombre, isDeleted, pageable);

        return PaginacionMapper.mapToResponse(aromasPage);
    }

    /**
     * Verifica si un nombre de aroma (exacto) ya está en uso.
     * @param nombre Nombre a verificar.
     * @return AvailabilityResponse.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkNombreAvailability(String nombre) {
        // Validación de nulidad y de string vacío post-limpieza
        String cleanedNombre = (nombre != null) ? nombre.trim() : "";

        if (cleanedNombre.isEmpty()) {
            return new AvailabilityResponse(false, "El nombre es obligatorio y no puede estar vacío.");
        }

        // Se busca por nombre exacto ignorando mayúsculas, sin importar si está activo o no.
        boolean existe = !aromaRepository.findByNombreIgnoreCase(cleanedNombre).isEmpty();

        if (!existe) {
            return new AvailabilityResponse(true, "Nombre disponible.");
        } else {
            return new AvailabilityResponse(
                    false,
                    " El nombre exacto '" + nombre + "' está en uso por algún aroma. Ingrese otro.");
        }
    }



    // --- MÉTODOS INTERNOS / HELPERS ---

    /**
     *  Verifica si un nombre es único. En caso de no serlo, lanza excepción.
     * @param nombre String con nombre a validar unicidad.
     */
    private void validarNombreUnico(String nombre) {
        if (!aromaRepository.findByNombreIgnoreCase(nombre).isEmpty()) {
            throw new ExistsRegisterException("Ya existe un aroma (activo o inactivo) con nombre " + nombre + ". Ingrese otro.");
        }
    }

    /**
     * Obtiene la entidad Aroma por su ID.
     * Usado internamente para validaciones.
     * @param id ID del aroma.
     * @return La entidad Aroma.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    public Aroma obtenerAromaRealPorId(Long id) {
        return aromaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe aroma con id " + id));
    }

    /**
     * Helper privado para mapear listas de Aroma a AromaResponse.
     * @param aromas Lista de entidades.
     * @return Lista de DTOs.
     */
    private List<AromaResponse> mapToList(List<Aroma> aromas) {
        return aromas.stream()
                .map(aromaMapper::toResponse)
                .toList();
    }
}
