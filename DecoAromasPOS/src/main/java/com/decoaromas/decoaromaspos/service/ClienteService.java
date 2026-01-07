package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.cliente.ClienteFilterDTO;
import com.decoaromas.decoaromaspos.dto.cliente.ClienteRequest;
import com.decoaromas.decoaromaspos.dto.cliente.ClienteResponse;
import com.decoaromas.decoaromaspos.dto.other.*;
import com.decoaromas.decoaromaspos.dto.other.request.EmailRequest;
import com.decoaromas.decoaromaspos.dto.other.request.RutRequest;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.ClienteMapper;
import com.decoaromas.decoaromaspos.model.Cliente;
import com.decoaromas.decoaromaspos.repository.ClienteRepository;
import com.decoaromas.decoaromaspos.utils.AvailabilityChecker;
import com.decoaromas.decoaromaspos.utils.ClienteSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AvailabilityChecker checker;
    private final ClienteMapper clienteMapper;


    // --- CONSULTAS (Lectura) ---

    /**
     * Obtiene una lista de todos los clientes (activos e inactivos).
     * @return Lista de ClienteResponse.
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientes() {
        return mapToList(clienteRepository.findAll());
    }

    /**
     * Obtiene una lista de todos los clientes marcados como 'activos'.
     * @return Lista de ClienteResponse.
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientesActivos() {
        return mapToList(clienteRepository.findByActivoTrue());
    }

    /**
     * Obtiene una lista de todos los clientes marcados como 'inactivos'.
     * @return Lista de ClienteResponse.
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarClientesInactivos() {
        return mapToList(clienteRepository.findByActivoFalse());
    }

    /**
     * Obtiene un cliente por su ID.
     * @param id El ID del cliente.
     * @return ClienteResponse.
     * @throws ResourceNotFoundException si no se encuentra el cliente.
     */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorId(Long id) {
        return clienteMapper.toResponse(obtenerClienteRealPorId(id));
    }

    /**
     * Obtiene un cliente por su RUT.
     * @param rutRequest DTO que contiene el RUT.
     * @return ClienteResponse.
     * @throws ResourceNotFoundException si no se encuentra el cliente.
     */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorRut(RutRequest rutRequest) {
        return clienteMapper.toResponse(clienteRepository.findByRutIgnoreCase(rutRequest.getRut())
                .orElseThrow(() -> new ResourceNotFoundException("No existe cliente con rut " + rutRequest.getRut())));
    }

    /**
     * Obtiene un cliente por su correo electrónico.
     * @param correoRequest DTO que contiene el correo.
     * @return ClienteResponse.
     * @throws ResourceNotFoundException si no se encuentra el cliente.
     */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorCorreo(EmailRequest correoRequest) {
        return clienteMapper.toResponse(clienteRepository.findByCorreoIgnoreCase(correoRequest.getCorreo())
                .orElseThrow(() -> new ResourceNotFoundException("No existe cliente con correo " + correoRequest.getCorreo())));
    }

    /**
     * Busca clientes (activos e inactivos) cuyo nombre contenga el texto proveído.
     * @param nombre Texto a buscar en el nombre.
     * @return Lista de ClienteResponse.
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarClientesPorNombreParcial(String nombre) {
        return mapToList(clienteRepository.findByNombreContainingIgnoreCase(nombre));
    }

    /**
     * Busca clientes (activos e inactivos) por nombre y apellido.
     * @param nombre   Texto a buscar en el nombre.
     * @param apellido Texto a buscar en el apellido.
     * @return Lista de ClienteResponse.
     */
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarClientesPorNombreYApellidoParcial(String nombre, String apellido) {
        return mapToList(clienteRepository.findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase(nombre, apellido));
    }

    /**
     * Obtiene clientes paginados y filtrados dinámicamente usando Specification.
     * @param page   Número de página (base 0).
     * @param size   Tamaño de la página.
     * @param sortBy Campo para ordenar.
     * @param dto    DTO con los filtros a aplicar.
     * @return PaginacionResponse con los resultados.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<ClienteResponse> getClientesFiltradosPaginados(int page, int size, String sortBy, ClienteFilterDTO dto) {
        Specification<Cliente> filtros = ClienteSpecification.conFiltros(dto);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        Page<Cliente> clientesPage = clienteRepository.findAll(filtros, pageable);
        Page<ClienteResponse> responsePage = clientesPage.map(clienteMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }


    // --- ESCRITURA (Creación, Actualización, Borrado) ---

    /**
     * Crea un nuevo cliente en el sistema.
     * Validar la unicidad de RUT, correo y teléfono.
     * @param clienteRequest DTO con la información del cliente.
     * @return ClienteResponse del cliente creado.
     * @throws ExistsRegisterException si RUT, correo o teléfono ya existen.
     */
    public ClienteResponse crearCliente(ClienteRequest clienteRequest) {
        Cliente cliente = new Cliente();

        setAndValidateClienteData(cliente, clienteRequest); // Delegar la validación y el seteo de datos al helper
        cliente.setActivo(true); // Lógica específica de creación

        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    /**
     * Actualiza un cliente existente.
     * Valida la unicidad de RUT, correo y teléfono (solo si han cambiado).
     * @param id             ID del cliente a actualizar.
     * @param clienteRequest DTO con los nuevos datos.
     * @return ClienteResponse del cliente actualizado.
     * @throws ResourceNotFoundException si el cliente no existe.
     * @throws ExistsRegisterException si RUT, correo o teléfono ya existen en *otro* cliente.
     */
    public ClienteResponse actualizarCliente(Long id, ClienteRequest clienteRequest) {
        Cliente existente = obtenerClienteRealPorId(id);

        setAndValidateClienteData(existente, clienteRequest); // Delegar la validación y el seteo de datos al helper

        return clienteMapper.toResponse(clienteRepository.save(existente));
    }

    /**
     * Cambia el estado (activo/inactivo) de un cliente.
     * @param id     ID del cliente.
     * @param activo Nuevo estado (true = activo, false = inactivo).
     * @return ClienteResponse actualizado.
     * @throws ResourceNotFoundException si el cliente no existe.
     */
    public ClienteResponse cambiarEstadoActivo(Long id, Boolean activo) {
        Cliente existente = obtenerClienteRealPorId(id);
        existente.setActivo(activo);
        return clienteMapper.toResponse(clienteRepository.save(existente));
    }


    // --- VALIDACIONES DE DISPONIBILIDAD (Para UI) ---

    /**
     * Elimina físicamente un cliente de la base de datos.
     * ¡ADVERTENCIA! Borrado físico.
     * No debería poder eliminar cliente nunca. Persistencia de datos
     * @param id ID del cliente a eliminar.
     * @throws ResourceNotFoundException si el cliente no existe.
     */
    public void eliminarCliente(Long id) {
        Cliente cliente = obtenerClienteRealPorId(id);
        // Validaciones de persistencia de datos
        clienteRepository.delete(cliente);
    }

    /**
     * Verifica la disponibilidad de un correo (para UI).
     * @param correo Correo a verificar.
     * @return AvailabilityResponse (DTO para el frontend).
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkCorreoAvailability(String correo) {
        String cleanedCorreo = correo.trim();
        return checker.check(() -> clienteRepository.existsByCorreoIgnoreCase(cleanedCorreo), "Correo", correo);
    }

    /**
     * Verifica la disponibilidad de un RUT (para UI).
     * @param rut RUT a verificar.
     * @return AvailabilityResponse (DTO para el frontend).
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkRutAvailability(String rut) {
        String cleanedRut = rut.trim();
        return checker.check(() -> clienteRepository.existsByRutIgnoreCase(cleanedRut), "RUT", rut);
    }

    /**
     * Verifica la disponibilidad de un teléfono (para UI).
     * @param telefono Teléfono a verificar.
     * @return AvailabilityResponse (DTO para el frontend).
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkTelefonoAvailability(String telefono) {
        String cleanedTelefono = telefono.trim();
        return checker.check(() -> clienteRepository.existsByTelefono(cleanedTelefono), "Teléfono", telefono);
    }


    // --- MÉTRICAS ---

    /**
     * Cuenta el número total de clientes activos.
     * @return Mapa de (String, Double) con Conteo de clientes activos.
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getCantidadClientesActivos() {
        Double cantidad = clienteRepository.countClienteByActivoTrue();
        return Map.of("cantidadClientesActivos", cantidad);
    }




    // --- LÓGICA INTERNA / HELPERS ---

    /**
     * Método helper centralizado para setear y validar los datos de un cliente.
     * Se usa para crear y actualizar.
     * ClienteRequest DTO ya ha limpiado y normalizado los datos.
     * @param cliente        La entidad Cliente (nueva o existente).
     * @param clienteRequest El DTO con los datos YA LIMPIOS.
     */
    private void setAndValidateClienteData(Cliente cliente, ClienteRequest clienteRequest) {
        // ID a excluir en las validaciones (null si es un cliente nuevo)
        Long idExcluido = cliente.getClienteId();

        // --- Validar y setear RUT (Campo obligatorio) ---
        String rut = clienteRequest.getRut();             // El DTO ya limpió, normalizó y validó @NotBlank
        // Solo validar si el RUT cambió (o si es nuevo)
        if (idExcluido == null || !rut.equalsIgnoreCase(cliente.getRut())) {
            validarRutUnico(rut, idExcluido);
        }
        cliente.setRut(rut);

        // --- Validar y setear Correo (Campo opcional) ---
        String correo = clienteRequest.getCorreo();         // El DTO ya limpió o convirtió a null
        // Solo validar si el Correo cambió (o si es nuevo)
        if (idExcluido == null || !Objects.equals(correo, cliente.getCorreo())) {
            validarCorreoUnico(correo, idExcluido);
        }
        cliente.setCorreo(correo);

        // --- Validar y setear Teléfono (Campo opcional) ---
        String telefono = clienteRequest.getTelefono();     // El DTO ya limpió o convirtió a null
        // Solo validar si el Teléfono cambió (o si es nuevo)
        if (idExcluido == null || !Objects.equals(telefono, cliente.getTelefono())) {
            validarTelefonoUnico(telefono, idExcluido);
        }
        cliente.setTelefono(telefono);

        // --- Setear campos no únicos ---
        cliente.setNombre(clienteRequest.getNombre());
        cliente.setApellido(clienteRequest.getApellido());
        cliente.setCiudad(clienteRequest.getCiudad());
        cliente.setTipo(clienteRequest.getTipo());
    }

    /**
     * Valida si el RUT ya existe para otro cliente.
     * @param rut El RUT a validar.
     * @param id El Id del cliente actual (null si es un cliente nuevo, para excluirlo de la búsqueda).
     */
    private void validarRutUnico(String rut, Long id) {
        boolean existe;
        if (id == null) {
            // Modo Creación
            existe = clienteRepository.existsByRutIgnoreCase(rut);
        } else {
            // Modo Actualización: busca un RUT igual en un ID diferente
            existe = clienteRepository.existsByRutIgnoreCaseAndClienteIdNot(rut, id);
        }

        if (existe) {
            throw new ExistsRegisterException("El RUT " + rut + " ya está registrado.");
        }
    }

    /**
     * Valida si el Correo (opcional) ya existe para otro cliente.
     * @param correo El correo a validar.
     * @param id El ID del cliente actual (null si es un cliente nuevo).
     */
    private void validarCorreoUnico(String correo, Long id) {
        // Si el correo es nulo, no se valida (el DTO ya se encargó de los string vacíos)
        if (correo == null) {
            return;
        }

        boolean existe;
        if (id == null) {
            existe = clienteRepository.existsByCorreoIgnoreCase(correo);
        } else {
            existe = clienteRepository.existsByCorreoIgnoreCaseAndClienteIdNot(correo, id);
        }

        if (existe) {
            throw new ExistsRegisterException("El correo " + correo + " ya está registrado.");
        }
    }

    /**
     * Valida si el Teléfono (opcional) ya existe para otro cliente.
     * @param telefono El teléfono a validar.
     * @param id El ID del cliente actual (null si es un cliente nuevo).
     */
    private void validarTelefonoUnico(String telefono, Long id) {
        // Si el teléfono es nulo o vacío, no se valida (ya que es opcional)
        if (telefono == null) {
            return;
        }

        boolean existe;
        if (id == null) {
            existe = clienteRepository.existsByTelefono(telefono);
        } else {
            existe = clienteRepository.existsByTelefonoAndClienteIdNot(telefono, id);
        }

        if (existe) {
            throw new ExistsRegisterException("El teléfono " + telefono + " ya está registrado.");
        }
    }

    /**
     * Método de utilidad (público) para obtener la *entidad* Cliente por su ID.
     * Usado por otros servicios (como VentaService) para obtener la entidad real.
     * @param id El ID del cliente.
     * @return La entidad Cliente.
     * @throws ResourceNotFoundException si no se encuentra el cliente.
     */
    public Cliente obtenerClienteRealPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe cliente con id " + id));
    }

    /**
     * Helper privado para mapear listas de entidades a DTOs.
     * @param clientes Lista de entidades Cliente.
     * @return Lista de ClienteResponse.
     */
    private List<ClienteResponse> mapToList(List<Cliente> clientes) {
        return clientes.stream().map(clienteMapper::toResponse).toList();
    }
}
