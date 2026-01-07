package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.dto.other.request.EmailRequest;
import com.decoaromas.decoaromaspos.dto.other.PaginacionMapper;
import com.decoaromas.decoaromaspos.dto.other.response.PaginacionResponse;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.usuario.*;
import com.decoaromas.decoaromaspos.enums.Rol;
import com.decoaromas.decoaromaspos.exception.ExistsRegisterException;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.mapper.UsuarioMapper;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.utils.AvailabilityChecker;
import com.decoaromas.decoaromaspos.utils.UsuarioSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.decoaromas.decoaromaspos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de Usuarios y lógica de permisos.
 * Se encarga del CRUD de usuarios, validaciones de unicidad,
 * y la lógica de negocio para la gestión de roles y contraseñas.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AvailabilityChecker checker;


    // --- CONSULTAS (Lectura) ---

    /**
     * Obtiene una lista de todos los usuarios (activos e inactivos). Excluye al Super Admin
     * @return Lista de UsuarioResponse.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosNoSuperAdmin() {
        return mapToList(usuarioRepository.findByRolIsNotLike(Rol.SUPER_ADMIN));
    }

    /**
     * Obtiene una lista de todos los usuarios (activos e inactivos).
     * @return Lista de UsuarioResponse.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        return mapToList(usuarioRepository.findAll());
    }

    /**
     * Obtiene una lista de todos los usuarios marcados como 'activos'.
     * @return Lista de UsuarioResponse.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosActivos() {
        return mapToList(usuarioRepository.findByActivoTrue());
    }

    /**
     * Obtiene una lista de todos los usuarios marcados como 'inactivos'.
     * @return Lista de UsuarioResponse.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosInactivos() {
        return mapToList(usuarioRepository.findByActivoFalse());
    }

    /**
     * Obtiene un usuario por su ID.
     * @param id El ID del usuario.
     * @return UsuarioResponse.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorId(Long id) {
        return usuarioMapper.toResponse(obtenerUsuarioRealPorId(id));
    }

    /**
     * Obtiene un usuario por su 'username' (insensible a mayúsculas).
     * @param username El nombre de usuario.
     * @return UsuarioResponse.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorUsername(String username) {
        return usuarioMapper.toResponse(usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + username + " no encontrado")));
    }

    /**
     * Obtiene un usuario por su correo electrónico.
     * @param correoRequest DTO con el correo.
     * @return UsuarioResponse.
     * @throws ResourceNotFoundException si no se encuentra.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorCorreo(EmailRequest correoRequest) {
        return usuarioMapper.toResponse(usuarioRepository.findByCorreo(correoRequest.getCorreo())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con correo " + correoRequest.getCorreo() + " no encontrado")));
    }

    /**
     * Busca usuarios por nombre (parcial, insensible a mayúsculas).
     * @param nombre Texto a buscar.
     * @return Lista de UsuarioResponse.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> buscarUsuariosPorNombreParcial(String nombre) {
        return mapToList(usuarioRepository.findByNombreContainingIgnoreCase(nombre));
    }

    /**
     * Busca usuarios por nombre y apellido (parcial, insensible a mayúsculas).
     * @param nombre Texto a buscar en nombre.
     * @param apellido Texto a buscar en apellido.
     * @return Lista de UsuarioResponse.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> buscarUsuariosPorNombreYApellidoParcial(String nombre, String apellido) {
        return mapToList(usuarioRepository.findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase(nombre, apellido));
    }

    /**
     * Obtiene usuarios paginados y filtrados dinámicamente.
     * @param page   Número de página (base 0).
     * @param size   Tamaño de la página.
     * @param sortBy Campo de ordenamiento.
     * @param dto    DTO con los filtros a aplicar.
     * @return PaginacionResponse con los usuarios.
     */
    @Transactional(readOnly = true)
    public PaginacionResponse<UsuarioResponse> getUsuariosFiltradosPaginados(int page, int size, String sortBy, UsuarioFilterDTO dto) {
        Specification<Usuario> filtros = UsuarioSpecification.conFiltros(dto);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        Page<Usuario> usuariosPage = usuarioRepository.findAll(filtros, pageable);
        Page<UsuarioResponse> responsePage = usuariosPage.map(usuarioMapper::toResponse);
        return PaginacionMapper.mapToResponse(responsePage);
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * Valida la unicidad del username y correo.
     * Encripta la contraseña.
     * @param request DTO con los datos limpios del nuevo usuario.
     * @return UsuarioResponse del usuario creado.
     * @throws ExistsRegisterException si el username o correo ya existen.
     */
    public UsuarioResponse registrarUsuario(UsuarioRequest request) {
        // --- Validación de unicidad ---
        validarUsernameUnico(request.getUsername(), null);
        validarCorreoUnico(request.getCorreo(), null);

        Usuario usuario = new Usuario();
        // Los datos que vienen de 'request' ya están limpios
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setCorreo(request.getCorreo());
        usuario.setUsername(request.getUsername());
        usuario.setRol(request.getRol());
        usuario.setActivo(true);

        // Encriptar password antes de guardar
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Actualiza el perfil de un usuario (nombre, apellido, correo, username).
     * No permite actualizar el Rol.
     * Aplica reglas de permiso de gestión.
     * @param id      ID del usuario a modificar (target).
     * @param request DTO con los nuevos datos.
     * @return UsuarioResponse actualizado.
     * @throws AccessDeniedException si el actor no tiene permiso para modificar al target.
     * @throws ExistsRegisterException si el nuevo username o correo ya existen.
     */
    public UsuarioResponse actualizarUsuarioNoRol(Long id, UsuarioUpdateRequest request) {
        Usuario actor = getCurrentAuthenticatedUser(); // Quién hace la petición
        Usuario existente = obtenerUsuarioRealPorId(id);  // A quién se modifica
        checkManagementPermission(actor, existente); // Validación permisos

        // --- Validación de unicidad (si los campos cambiaron) ---
        if (!existente.getUsername().equalsIgnoreCase(request.getUsername())) {
            validarUsernameUnico(request.getUsername(), id);
        }
        if (!existente.getCorreo().equalsIgnoreCase(request.getCorreo())) {
            validarCorreoUnico(request.getCorreo(), id);
        }

        existente.setNombre(request.getNombre());
        existente.setApellido(request.getApellido());
        existente.setCorreo(request.getCorreo());
        existente.setUsername(request.getUsername());

        return usuarioMapper.toResponse(usuarioRepository.save(existente));
    }

    /**
     * Actualiza el Rol de un usuario (target).
     * Aplica lógica de negocio estricta sobre quién puede cambiar qué rol.
     * @param request DTO con el ID del usuario (target) y el nuevo Rol.
     * @return UsuarioResponse actualizado.
     * @throws AccessDeniedException si la operación de cambio de rol no está permitida.
     */
    public UsuarioResponse actualizarRolDeUsuario(UsuarioUpdateRol request) {
        Usuario actor = getCurrentAuthenticatedUser();
        Usuario target = obtenerUsuarioRealPorId(request.getUsuarioId());

        // Un usuario no puede cambiar su propio rol.
        if (actor.getUsuarioId().equals(target.getUsuarioId())) {
            throw new AccessDeniedException("No puedes cambiar tu propio rol.");
        }
        // Nadie puede modificar al SUPER_ADMIN.
        if (target.getRol() == Rol.SUPER_ADMIN) {
            throw new AccessDeniedException("El usuario SUPER_ADMIN no puede ser modificado.");
        }
        // Nadie puede asignar el rol de SUPER_ADMIN.
        if (request.getRol() == Rol.SUPER_ADMIN) {
            throw new AccessDeniedException("El rol SUPER_ADMIN no puede ser asignado.");
        }
        // Un ADMIN solo puede gestionar a VENDEDORES.
        if (actor.getRol() == Rol.ADMIN && target.getRol() != Rol.VENDEDOR) {
            throw new AccessDeniedException("Los administradores solo pueden gestionar a vendedores.");
        }

        target.setRol(request.getRol());
        return usuarioMapper.toResponse(usuarioRepository.save(target));
    }

    /**
     * Cambia el estado (activo/inactivo) de un usuario.
     * Aplica reglas de permiso de gestión.
     * @param id     ID del usuario a modificar (target).
     * @param activo Nuevo estado.
     * @return UsuarioResponse actualizado.
     * @throws AccessDeniedException si el actor no tiene permiso para modificar al target.
     */
    public UsuarioResponse cambiarEstadoActivo(Long id, Boolean activo) {
        Usuario actor = getCurrentAuthenticatedUser();
        Usuario existenteTarget = obtenerUsuarioRealPorId(id);
        checkManagementPermission(actor, existenteTarget); // Validación permisos

        existenteTarget.setActivo(activo);
        return usuarioMapper.toResponse(usuarioRepository.save(existenteTarget));
    }

    /**
     * Actualiza la contraseña del *propio* del usuario según id
     * @param id Id de usuario a actualizar password
     * @param request DTO con la contraseña actual y la nueva.
     * @throws IllegalArgumentException si la contraseña actual es incorrecta.
     */
    public void actualizarMiPassword(Long id, UsuarioPasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe usuario con id " + id));

        if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }
        if (request.getPasswordNueva() == null || request.getPasswordNueva().length() < 4) {
            throw new IllegalArgumentException("La contraseña nueva es muy corta.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);
    }

    /**
     * Elimina físicamente un usuario. (Hard Delete)
     * ¡Advertencia! Puede fallar por constraints si el usuario tiene ventas, etc.
     * No es posible eliminar un usuario desde el frontend
     * @param id ID del usuario a eliminar.
     */
    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerUsuarioRealPorId(id);
        // Faltan validaciones de seguridad (ej.: no eliminar SUPER_ADMIN)
        // o de integridad (ej.: si tiene ventas, no eliminar).
        usuarioRepository.delete(usuario);
    }

    // --- VALIDACIONES DE DISPONIBILIDAD (Para UI) ---

    /**
     * Verifica la disponibilidad de un correo (para UI).
     * @param correo Correo a verificar.
     * @return AvailabilityResponse.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkCorreoAvailability(String correo) {
        String cleanedCorreo = correo.trim();
        return checker.check(() -> usuarioRepository.existsByCorreoIgnoreCase(cleanedCorreo), "Correo", correo);
    }

    /**
     * Verifica la disponibilidad de un username (para UI).
     * @param username Username a verificar.
     * @return AvailabilityResponse.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkUsernameAvailability(String username) {
        String cleanedUsername = username.trim();
        return checker.check(() -> usuarioRepository.existsByUsernameIgnoreCase(cleanedUsername), "Username", username);
    }


    // --- LÓGICA INTERNA / HELPERS ---

    /**
     * Obtiene la entidad Usuario completa del usuario actualmente autenticado por el SecurityContext.
     * @return La entidad Usuario del actor.
     * @throws UsernameNotFoundException si el usuario del token no se encuentra en BD.
     */
    private Usuario getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("No hay un usuario autenticado.");
        }
        String currentUsername = authentication.getName();
        return usuarioRepository.findByUsernameIgnoreCase(currentUsername)
                .or(() -> usuarioRepository.findByCorreo(currentUsername)) // Asume que se puede loguear con username o correo
                .orElseThrow(() -> new UsernameNotFoundException("Usuario autenticado no encontrado en la base de datos: " + currentUsername));
    }

    /**
     * Valida si un usuario (actor) tiene permiso para gestionar a otro (target).
     * Aplica para acciones generales como editar perfil o cambiar estado.
     * @param actor  El usuario que realiza la acción (autenticado).
     * @param target El usuario que será modificado.
     * @throws AccessDeniedException si la acción no está permitida.
     */
    private void checkManagementPermission(Usuario actor, Usuario target) {
        // REGLA: Si soy yo mismo, tengo permiso (Cortocircuito)
        // Un usuario no puede modificarse a sí mismo a través de estos endpoints de gestión.
        if (actor.getUsuarioId().equals(target.getUsuarioId())) {
            return; // Salida, validación exitosa.
        }
        // Nadie puede modificar al SUPER_ADMIN.
        if (target.getRol() == Rol.SUPER_ADMIN) {
            throw new AccessDeniedException("El usuario SUPER_ADMIN no puede ser modificado.");
        }
        // Un ADMIN solo puede gestionar a VENDEDORES. No otros ADMINS o SUPER_ADMINS
        if (actor.getRol() == Rol.ADMIN && target.getRol() != Rol.VENDEDOR) {
            throw new AccessDeniedException("Como Administrador, solo puedes gestionar a usuarios con el rol de Vendedor.");
        }
        // Un VENDEDOR no puede gestionar a nadie.
        if (actor.getRol() == Rol.VENDEDOR) {
            throw new AccessDeniedException("Como Vendedor, no tienes permisos de gestión de usuarios.");
        }
    }

    /**
     * Valida si el Correo ya existe para otro usuario.
     * @param correo El correo a validar (obligatorio).
     * @param idExcluir El ID del usuario actual (null si es un usuario nuevo).
     */
    private void validarCorreoUnico(String correo, Long idExcluir) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo no puede estar vacío."); // Defensa adicional, @NotBlank ya lo valida
        }
        boolean existe;
        if (idExcluir == null) {
            existe = usuarioRepository.existsByCorreoIgnoreCase(correo);
        } else {
            existe = usuarioRepository.existsByCorreoIgnoreCaseAndUsuarioIdNot(correo, idExcluir);
        }
        if (existe) {
            throw new ExistsRegisterException("El correo " + correo + " ya está registrado.");
        }
    }

    /**
     * Valida si el Username ya existe para otro usuario.
     * @param username El username a validar.
     * @param idExcluir El ID del usuario actual (null si es un usuario nuevo).
     */
    private void validarUsernameUnico(String username, Long idExcluir) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío.");
        }
        boolean existe;
        if (idExcluir == null) {
            existe = usuarioRepository.existsByUsernameIgnoreCase(username);
        } else {
            existe = usuarioRepository.existsByUsernameIgnoreCaseAndUsuarioIdNot(username, idExcluir);
        }
        if (existe) {
            throw new ExistsRegisterException("El username " + username + " ya está registrado.");
        }
    }

    /**
     * Método de utilidad (público) para obtener la *entidad* Usuario por su ID.
     * Usado por otros servicios (como VentaService, CajaService) para
     * obtener la entidad real y no el DTO.
     * @param id El ID del usuario.
     * @return La entidad Usuario.
     * @throws ResourceNotFoundException si no se encuentra el usuario.
     */
    public Usuario obtenerUsuarioRealPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe usuario con id " + id));
    }

    /**
     * Helper privado para mapear listas de entidades a DTOs.
     * @param usuarios Lista de entidades Usuario.
     * @return Lista de UsuarioResponse.
     */
    private List<UsuarioResponse> mapToList(List<Usuario> usuarios) {
        return usuarios.stream().map(usuarioMapper::toResponse).toList();
    }
}
