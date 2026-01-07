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
import com.decoaromas.decoaromaspos.repository.UsuarioRepository;
import com.decoaromas.decoaromaspos.utils.AvailabilityChecker;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Supplier;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @InjectMocks
    private UsuarioService serviceUser;
    @Mock
    private UsuarioRepository usuarioRepository;
    private UsuarioService serviceUserSpy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        serviceUserSpy = spy(serviceUser);
        ReflectionTestUtils.setField(serviceUserSpy, "usuarioRepository", usuarioRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Test para listar a usuarios sin Super Admin")
    void listarUsuarios_NoSuperAdmin_deberiaDevolverTodos() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        usuario.setUsername("juanp");
        usuario.setCorreo("juan@gmail.com");
        usuario.setRol(Rol.VENDEDOR);
        usuario.setActivo(true);

        when(mockRepo.findByRolIsNotLike(Rol.SUPER_ADMIN)).thenReturn(List.of(usuario));
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            UsuarioResponse r = new UsuarioResponse();
            r.setUsuarioId(u.getUsuarioId());
            r.setNombre(u.getNombre());
            r.setApellido(u.getApellido());
            r.setCorreo(u.getCorreo());
            r.setUsername(u.getUsername());
            r.setRol(u.getRol());
            r.setActivo(u.getActivo());
            return r;
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        List<UsuarioResponse> usuarios = service.listarUsuariosNoSuperAdmin();
        assertNotNull(usuarios);
        assertEquals(1, usuarios.size());
        assertEquals("Juan", usuarios.get(0).getNombre());
        verify(mockRepo).findByRolIsNotLike(Rol.SUPER_ADMIN);
    }

    @Test
    @DisplayName("Test para listar a usuarios")
    void listarUsuarios_deberiaDevolverTodos() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setNombre("Juan");
        usuario.setApellido("Perez");
        usuario.setUsername("juanp");
        usuario.setCorreo("juan@gmail.com");
        usuario.setRol(Rol.VENDEDOR);
        usuario.setActivo(true);

        when(mockRepo.findAll()).thenReturn(List.of(usuario));
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            UsuarioResponse r = new UsuarioResponse();
            r.setUsuarioId(u.getUsuarioId());
            r.setNombre(u.getNombre());
            r.setApellido(u.getApellido());
            r.setCorreo(u.getCorreo());
            r.setUsername(u.getUsername());
            r.setRol(u.getRol());
            r.setActivo(u.getActivo());
            return r;
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        List<UsuarioResponse> usuarios = service.listarUsuarios();
        assertNotNull(usuarios);
        assertEquals(1, usuarios.size());
        assertEquals("Juan", usuarios.get(0).getNombre());
        verify(mockRepo).findAll();
    }

    @Test
    @DisplayName("Test para obtener un usuario por ID si existe")
    void obtenerUsuarioPorId_usuarioExiste_deberiaDevolverResponse() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setNombre("Ana");
        usuario.setApellido("Gomez");

        when(mockRepo.findById(1L)).thenReturn(Optional.of(usuario));

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .build();
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioResponse response = service.obtenerUsuarioPorId(1L);
        assertNotNull(response);
        assertEquals("Ana", response.getNombre());
        assertEquals("Gomez", response.getApellido());
        verify(mockRepo).findById(1L);
    }

    @Test
    @DisplayName("Test para obtener un usuario por ID, si no existe lanza excepcion")
    void obtenerUsuarioPorId_usuarioNoExiste_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        when(mockRepo.findById(1L)).thenReturn(Optional.empty());
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerUsuarioPorId(1L));
        verify(mockRepo).findById(1L);
    }

    @Test
    @DisplayName("Test para registrar un usuario, debe guardarlo")
    void registrarUsuario_deberiaGuardarUsuario() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Luis");
        request.setApellido("Diaz");
        request.setCorreo("luis@mail.com");
        request.setUsername("luisd");
        request.setRol(Rol.ADMIN);
        request.setPassword("1234");

        Usuario saved = new Usuario();
        saved.setUsuarioId(1L);
        saved.setNombre(request.getNombre());
        saved.setApellido(request.getApellido());
        saved.setUsername(request.getUsername());
        saved.setCorreo(request.getCorreo());
        saved.setRol(request.getRol());
        saved.setActivo(true);
        saved.setPassword(encoder.encode(request.getPassword()));

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .rol(u.getRol())
                    .activo(u.getActivo())
                    .nombreCompleto(u.getNombre() + " " + u.getApellido())
                    .build();
        });

        when(mockRepo.save(any(Usuario.class))).thenReturn(saved);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioResponse response = service.registrarUsuario(request);
        assertNotNull(response);
        assertEquals("Luis", response.getNombre());
        assertTrue(response.getActivo());
        verify(mockRepo).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Test para registrar un usuario con correo, debe guardarlo")
    void registrarUsuarioConCorreo_deberiaGuardarUsuario() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Luis");
        request.setApellido("Diaz");
        request.setCorreo("lucho@mail.com");
        request.setUsername("luisd");
        request.setRol(Rol.ADMIN);
        request.setPassword("1234");

        Usuario saved = new Usuario();
        saved.setUsuarioId(1L);
        saved.setNombre(request.getNombre());
        saved.setApellido(request.getApellido());
        saved.setUsername(request.getUsername());
        saved.setCorreo(request.getCorreo());
        saved.setRol(request.getRol());
        saved.setActivo(true);
        saved.setPassword(encoder.encode(request.getPassword()));

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .rol(u.getRol())
                    .activo(u.getActivo())
                    .nombreCompleto(u.getNombre() + " " + u.getApellido())
                    .build();
        });

        when(mockRepo.save(any(Usuario.class))).thenReturn(saved);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioResponse response = service.registrarUsuario(request);
        assertNotNull(response);
        assertEquals("Luis", response.getNombre());
        assertEquals("Diaz", response.getApellido());
        assertEquals("lucho@mail.com", response.getCorreo());
        assertTrue(response.getActivo());
        verify(mockRepo).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Test para registrar un usuario con correo vacío, debe lanzar excepción")
    void registrarUsuarioCorreoVacio_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Luis");
        request.setApellido("Diaz");
        request.setCorreo("");
        request.setUsername("luisd");
        request.setRol(Rol.ADMIN);
        request.setPassword("1234");

        Usuario saved = new Usuario();
        saved.setUsuarioId(1L);
        saved.setNombre(request.getNombre());
        saved.setApellido(request.getApellido());
        saved.setUsername(request.getUsername());
        saved.setCorreo(request.getCorreo());
        saved.setRol(request.getRol());
        saved.setActivo(true);
        saved.setPassword(encoder.encode(request.getPassword()));

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .rol(u.getRol())
                    .activo(u.getActivo())
                    .nombreCompleto(u.getNombre() + " " + u.getApellido())
                    .build();
        });

        when(mockRepo.save(any(Usuario.class))).thenReturn(new Usuario());
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        // Act + Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.registrarUsuario(request));
        assertEquals("El correo no puede estar vacío.", exception.getMessage());

        // Verifica que el repositorio NUNCA fue llamado, ya que la validación falló antes
        verify(mockRepo, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Test para registrar un usuario con correo vacío , debe guardarlo")
    void registrarUsuarioCorreoVacioStrim_deberiaGuardarUsuario() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Luis");
        request.setApellido("Diaz");
        request.setCorreo(null);
        request.setUsername("luisd");
        request.setRol(Rol.ADMIN);
        request.setPassword("1234");

        Usuario saved = new Usuario();
        saved.setUsuarioId(1L);
        saved.setNombre(request.getNombre());
        saved.setApellido(request.getApellido());
        saved.setUsername(request.getUsername());
        saved.setCorreo(request.getCorreo());
        saved.setRol(request.getRol());
        saved.setActivo(true);
        saved.setPassword(encoder.encode(request.getPassword()));

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .rol(u.getRol())
                    .activo(u.getActivo())
                    .nombreCompleto(u.getNombre() + " " + u.getApellido())
                    .build();
        });

        when(mockRepo.save(any(Usuario.class))).thenReturn(new Usuario());
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        // Act + Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.registrarUsuario(request));
        assertEquals("El correo no puede estar vacío.", exception.getMessage());

        // Verifica que el repositorio NUNCA fue llamado, ya que la validación falló antes
        verify(mockRepo, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Test para actualizar una contraseña")
    void actualizarPassword_contraseñaCorrecta_deberiaActualizar() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setPassword(encoder.encode("actual")); // imaginemos que la contraseña es actual

        when(mockRepo.findById(1L)).thenReturn(Optional.of(usuario));
        when(mockRepo.save(any(Usuario.class))).thenReturn(usuario);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        UsuarioPasswordRequest request = new UsuarioPasswordRequest("actual", "nueva123");
        service.actualizarMiPassword(1L, request);
        assertTrue(encoder.matches("nueva123", usuario.getPassword()));
        verify(mockRepo).save(usuario);
    }

    @Test
    @DisplayName("Test para actualizar una contraseña, caso incorrecto")
    void actualizarPassword_contraseñaIncorrecta_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setPassword(new BCryptPasswordEncoder().encode("actual")); // imaginemos que la contraseña es actual
        when(mockRepo.findById(1L)).thenReturn(Optional.of(usuario));
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioPasswordRequest request = new UsuarioPasswordRequest("wrong", "nueva");
        assertThrows(IllegalArgumentException.class, () -> service.actualizarMiPassword(1L, request));
        verify(mockRepo, never()).save(usuario); // se lanza la exception y no se actualiza la contraseña
    }

    @Test
    @DisplayName("Test para actualizar una contraseña nula, caso incorrecto")
    void actualizarPassword_contraseñaNuevaNula_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setPassword(new BCryptPasswordEncoder().encode("actual")); // imaginemos que la contraseña es actual
        when(mockRepo.findById(1L)).thenReturn(Optional.of(usuario));
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioPasswordRequest request = new UsuarioPasswordRequest("actual", null);
        assertThrows(IllegalArgumentException.class, () -> service.actualizarMiPassword(1L, request));
        verify(mockRepo, never()).save(usuario); // se lanza la exception y no se actualiza la contraseña
    }

    @Test
    @DisplayName("Test para actualizar una contraseña muy corta, caso incorrecto")
    void actualizarPassword_contraseñaNuevaCorta_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setPassword(new BCryptPasswordEncoder().encode("actual")); // imaginemos que la contraseña es actual
        when(mockRepo.findById(1L)).thenReturn(Optional.of(usuario));
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioPasswordRequest request = new UsuarioPasswordRequest("actual", "12");
        assertThrows(IllegalArgumentException.class, () -> service.actualizarMiPassword(1L, request));
        verify(mockRepo, never()).save(usuario); // se lanza la exception y no se actualiza la contraseña
    }

    @Test
    @DisplayName("Test para eliminar un usuario, debe borrarlo")
    void eliminarUsuario_deberiaLlamarDelete() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(usuario));
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        service.eliminarUsuario(1L);
        verify(mockRepo).delete(usuario);
    }

    @Test
    @DisplayName("Test para cambiar estado activo/noActivo")
    void cambiarEstadoActivo_deberiaActualizarEstado() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("admin");
        actor.setRol(Rol.ADMIN);
        // Usuario objetivo (target)
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setUsername("user2");
        target.setActivo(true);
        target.setRol(Rol.VENDEDOR);
        // Simular autenticación real
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Simulación
        when(mockRepo.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(actor)); // autenticado
        when(mockRepo.findById(2L)).thenReturn(Optional.of(target)); // usuario a modificar
        when(mockRepo.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setActivo(false); // simula cambio de estado
            return u;
        });
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .username(u.getUsername())
                    .activo(u.getActivo())
                    .build();
        });
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioResponse response = service.cambiarEstadoActivo(2L, false);
        assertNotNull(response);
        assertFalse(response.getActivo());
        verify(mockRepo).save(any(Usuario.class));
        // Limpieza del contexto
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Test para cambiar estado activo de usuario, un vendedor no puede cambiar ningun estado, lanza AccessDeniedException")
    void cambiarEstadoActivo_vendedorNoPuedeCambiarEstado_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("vendedor1");
        actor.setRol(Rol.VENDEDOR);
        // Usuario objetivo (target)
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setUsername("user2");
        target.setActivo(true);
        target.setRol(Rol.VENDEDOR);
        // Simular autenticación real
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("vendedor1", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Simulación
        when(mockRepo.findByUsernameIgnoreCase("vendedor1")).thenReturn(Optional.of(actor)); // autenticado
        when(mockRepo.findById(2L)).thenReturn(Optional.of(target)); // usuario a modificar
        when(mockRepo.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setActivo(false); // simula cambio de estado
            return u;
        });
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .username(u.getUsername())
                    .activo(u.getActivo())
                    .build();
        });
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        // Act + Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> service.cambiarEstadoActivo(2L, false));

        assertEquals("Como Vendedor, no tienes permisos de gestión de usuarios.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para cambiar estado activo de usuario, no se puede modificar un super admin, lanza AccessDeniedException")
    void cambiarEstadoActivo_noSeCambiaASuperAdmin_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("vendedor1");
        actor.setRol(Rol.VENDEDOR);
        // Usuario objetivo (target)
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setUsername("user2");
        target.setActivo(true);
        target.setRol(Rol.SUPER_ADMIN);
        // Simular autenticación real
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("vendedor1", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Simulación
        when(mockRepo.findByUsernameIgnoreCase("vendedor1")).thenReturn(Optional.of(actor)); // autenticado
        when(mockRepo.findById(2L)).thenReturn(Optional.of(target)); // usuario a modificar
        when(mockRepo.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setActivo(false); // simula cambio de estado
            return u;
        });
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .username(u.getUsername())
                    .activo(u.getActivo())
                    .build();
        });
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> service.cambiarEstadoActivo(2L, false));

        assertEquals("El usuario SUPER_ADMIN no puede ser modificado.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para cambiar estado activo de usuario, un admin solo puede gestionar vendedores, lanza AccessDeniedException")
    void cambiarEstadoActivo_adminSoloGestionaVendedores_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("admin");
        actor.setRol(Rol.ADMIN);
        // Usuario objetivo (target)
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setUsername("user2");
        target.setActivo(true);
        target.setRol(Rol.ADMIN);
        // Simular autenticación real
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Simulación
        when(mockRepo.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(actor)); // autenticado
        when(mockRepo.findById(2L)).thenReturn(Optional.of(target)); // usuario a modificar
        when(mockRepo.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setActivo(false); // simula cambio de estado
            return u;
        });
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .username(u.getUsername())
                    .activo(u.getActivo())
                    .build();
        });
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        // Act + Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> service.cambiarEstadoActivo(2L, false));

        assertEquals("Como Administrador, solo puedes gestionar a usuarios con el rol de Vendedor.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para cambiar estado activo, un admin puede gestionarse a sí mismo según la lógica actual")
    void cambiarEstadoActivo_adminPuedeGestionarseASiMismo() {

        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("admin");
        actor.setRol(Rol.ADMIN);
        actor.setActivo(true);

        Usuario target = new Usuario();
        target.setUsuarioId(1L);
        target.setUsername("admin");
        target.setRol(Rol.ADMIN);
        target.setActivo(true);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(mockRepo.findByUsernameIgnoreCase("admin"))
                .thenReturn(Optional.of(actor));
        when(mockRepo.findById(1L))
                .thenReturn(Optional.of(target));

        // AvailabilityChecker retorna algo válido
        when(mockChecker.check(any(), anyString(), anyString()))
                .thenReturn(new AvailabilityResponse(true, "OK"));

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        assertDoesNotThrow(() -> service.cambiarEstadoActivo(1L, false));

        SecurityContextHolder.clearContext();
    }




    @Test
    @DisplayName("Test para obtener un usuario por nombre, si existe lo devuelve")
    void obtenerUsuarioPorUsername_existe_deberiaDevolverUsuario() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setUsername("juanp");
        when(mockRepo.findByUsernameIgnoreCase("juanp")).thenReturn(Optional.of(usuario));
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            UsuarioResponse r = new UsuarioResponse();
            r.setUsername(u.getUsername());
            return r;
        });
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        UsuarioResponse response = service.obtenerUsuarioPorUsername("juanp");
        assertNotNull(response);
        assertEquals("juanp", response.getUsername());
        verify(mockRepo).findByUsernameIgnoreCase("juanp");
    }

    @Test
    @DisplayName("Test para obtener un usuario por nombre, si no exite lanza excepcion")
    void obtenerUsuarioPorUsername_noExiste_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        when(mockRepo.findByUsernameIgnoreCase("noexiste")).thenReturn(Optional.empty());
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerUsuarioPorUsername("noexiste"));
    }

    @Test
    @DisplayName("Test para obtener un usuario por correo, si existe el correo devuelve al usuario")
    void obtenerUsuarioPorCorreo_existe_deberiaDevolverUsuario() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        // Usuario datos
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(1L);
        usuario.setCorreo("juan@gmail.com");
        // target
        when(mockRepo.findByCorreo("juan@gmail.com")).thenReturn(Optional.of(usuario));
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .correo(u.getCorreo())
                    .build();
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        EmailRequest request = new EmailRequest();
        request.setCorreo("juan@gmail.com");
        UsuarioResponse response = service.obtenerUsuarioPorCorreo(request);
        assertNotNull(response);
        assertEquals("juan@gmail.com", response.getCorreo());
        verify(mockRepo).findByCorreo("juan@gmail.com");
    }

    @Test
    @DisplayName("Test para obtener usuario por correo, si el correo no existe lanza una excepcion")
    void obtenerUsuarioPorCorreo_noExiste_deberiaLanzarExcepcion() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        when(mockRepo.findByCorreo("no@mail.com")).thenReturn(Optional.empty());
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        EmailRequest request = new EmailRequest();
        request.setCorreo("no@mail.com");
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerUsuarioPorCorreo(request));
    }

    @Test
    @DisplayName("Test para buscar usuario por nombre parcial, devuelve una lista con coincidencias")
    void buscarUsuariosPorNombreParcial_deberiaDevolverLista() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        // Usuarios
        Usuario u1 = new Usuario();
        u1.setNombre("Juan");
        Usuario u2 = new Usuario();
        u2.setNombre("Juana");

        when(mockRepo.findByNombreContainingIgnoreCase("Juan")).thenReturn(List.of(u1, u2));
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            UsuarioResponse r = new UsuarioResponse();
            r.setNombre(u.getNombre());
            return r;
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        List<UsuarioResponse> resultados = service.buscarUsuariosPorNombreParcial("Juan");
        assertNotNull(resultados);
        assertEquals(2, resultados.size());
        assertTrue(resultados.stream().anyMatch(r -> r.getNombre().equals("Juan")));
        verify(mockRepo).findByNombreContainingIgnoreCase("Juan");
    }

    @Test
    @DisplayName("Test para buscar usuario por nombre y apellido parcial, devuelve una lista con coincidencias")
    void buscarUsuariosPorNombreYApellidoParcial_deberiaDevolverLista() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        // Usuarios
        Usuario u1 = new Usuario();
        u1.setNombre("Juan");
        u1.setApellido("Perez");

        Usuario u2 = new Usuario();
        u2.setNombre("Juana");
        u2.setApellido("Perez");

        when(mockRepo.findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase("Juan", "Perez"))
                .thenReturn(List.of(u1, u2));
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .activo(u.getActivo())
                    .build();
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        List<UsuarioResponse> resultados = service.buscarUsuariosPorNombreYApellidoParcial("Juan", "Perez");
        assertNotNull(resultados);
        assertEquals(2, resultados.size());
        assertTrue(resultados.stream().anyMatch(r -> "Perez".equals(r.getApellido())));
        verify(mockRepo).findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase("Juan", "Perez");
    }

    @Test
    @DisplayName("Test para listar usuarios activos, devuelve solo los activos")
    void listarUsuariosNoSuperAdminActivos_deberiaDevolverSoloActivos() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        // Usuarios simulados
        Usuario u1 = new Usuario();
        u1.setNombre("Activo1");
        u1.setActivo(true);

        Usuario u2 = new Usuario();
        u2.setNombre("Activo2");
        u2.setActivo(true);

        Usuario u3 = new Usuario();
        u3.setNombre("Inactivo");
        u3.setActivo(false);

        when(mockRepo.findByActivoTrue()).thenReturn(List.of(u1, u2));
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .nombre(u.getNombre())
                    .activo(u.getActivo())
                    .build();
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        List<UsuarioResponse> resultados = service.listarUsuariosActivos();
        assertNotNull(resultados);
        assertEquals(2, resultados.size());
        assertTrue(resultados.stream().allMatch(UsuarioResponse::getActivo));
        verify(mockRepo).findByActivoTrue();
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, no puede cambiar su propio rol")
    void actualizarRolDeUsuario_noPuedeCambiarSuPropioRol() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("user");
        actor.setRol(Rol.ADMIN);

        Usuario target = new Usuario();
        target.setUsuarioId(1L);
        target.setRol(Rol.ADMIN);

        UsuarioUpdateRol request = new UsuarioUpdateRol(1L, Rol.VENDEDOR);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(target));
        when(mockRepo.findByUsernameIgnoreCase("user")).thenReturn(Optional.of(actor));
        // Simular usuario autenticado correctamente
        var auth = new TestingAuthenticationToken("user", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(AccessDeniedException.class, () -> service.actualizarRolDeUsuario(request));
        verify(mockRepo, never()).save(any());
        SecurityContextHolder.clearContext(); // Limpieza
    }

    @Test
    @DisplayName("Test para revisar la disponibilidad de un username existente, si existe devuelve que ya esta en uso")
    void checkUsernameAvailability_usernameYaExiste_deberiaDevolverNoDisponible() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker realChecker = new AvailabilityChecker();
        when(mockRepo.existsByUsernameIgnoreCase("user1")).thenReturn(true);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, realChecker);
        AvailabilityResponse response = service.checkUsernameAvailability("user1");
        assertNotNull(response);
        assertFalse(response.isAvailable());
        assertEquals("Username 'user1' ya en uso. Ingrese otro.", response.getMessage());
    }

    @Test
    @DisplayName("Test para revisar la disponibilidad de un username, si esta disponible lo indica")
    void checkUsernameAvailability_usernameDisponible_deberiaDevolverDisponible() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker realChecker = new AvailabilityChecker();
        when(mockRepo.existsByUsernameIgnoreCase("nuevoUser")).thenReturn(false);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, realChecker);
        AvailabilityResponse response = service.checkUsernameAvailability("nuevoUser");
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertEquals("Username disponible.", response.getMessage());
    }

    @SneakyThrows
    @Test
    @DisplayName("Test para revisar la disponibilidad de un nombre, con username limpio / autentificar")
    void checkUsernameAvailability_deberiaLlamarCheckerConUsernameLimpio() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        AvailabilityChecker checker = mock(AvailabilityChecker.class);
        // Inyectacción del mock
        Field checkerField = UsuarioService.class.getDeclaredField("checker");
        checkerField.setAccessible(true);
        checkerField.set(service, checker);

        String usernameOriginal = "   usuarioTest   ";
        String usernameLimpio = "usuarioTest";

        AvailabilityResponse expectedResponse =
                new AvailabilityResponse(false, "Username no disponible");

        when(checker.check(any(), eq("Username"), eq(usernameOriginal)))
                .thenReturn(expectedResponse);
        AvailabilityResponse response = service.checkUsernameAvailability(usernameOriginal);
        assertEquals(expectedResponse, response);
        // Capturar el Supplier y verificar llamada al repositorio
        ArgumentCaptor<Supplier<Boolean>> supplierCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(checker).check(supplierCaptor.capture(), eq("Username"), eq(usernameOriginal));
        Supplier<Boolean> supplier = supplierCaptor.getValue();
        when(mockRepo.existsByUsernameIgnoreCase(usernameLimpio)).thenReturn(true);
        supplier.get();
        verify(mockRepo).existsByUsernameIgnoreCase(usernameLimpio);
    }

    @Test
    @DisplayName("Test para listar los usuarios inactivos, retorna una lista de ellos")
    void listarUsuariosInactivos_deberiaRetornarListaDeUsuariosNoSuperAdminInactivos() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);

        // Crear usuarios inactivos simulados
        Usuario usuario1 = new Usuario();
        usuario1.setUsuarioId(1L);
        usuario1.setUsername("juan");
        usuario1.setActivo(false);
        usuario1.setRol(Rol.VENDEDOR);

        Usuario usuario2 = new Usuario();
        usuario2.setUsuarioId(2L);
        usuario2.setUsername("maria");
        usuario2.setActivo(false);
        usuario2.setRol(Rol.VENDEDOR);

        List<Usuario> usuariosInactivos = Arrays.asList(usuario1, usuario2);
        when(mockRepo.findByActivoFalse()).thenReturn(usuariosInactivos);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            UsuarioResponse r = new UsuarioResponse();
            r.setUsuarioId(u.getUsuarioId());
            r.setUsername(u.getUsername());
            r.setActivo(u.getActivo());
            return r;
        });

        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);
        List<UsuarioResponse> resultado = service.listarUsuariosInactivos();
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("juan", resultado.get(0).getUsername());
        assertEquals("maria", resultado.get(1).getUsername());
        assertFalse(resultado.get(0).getActivo());
        assertFalse(resultado.get(1).getActivo());
        verify(mockRepo, times(1)).findByActivoFalse();
    }

    @Test
    @DisplayName("Test para revisar la disponibilidad de un correo, si no existe el correo lo retorna como disponible")
    void checkCorreoAvailability_deberiaRetornarDisponibleCuandoNoExiste() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        when(mockRepo.existsByCorreoIgnoreCase("correo@test.com")).thenReturn(false);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker realChecker = new AvailabilityChecker();
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, realChecker);
        AvailabilityResponse response = service.checkCorreoAvailability("  correo@test.com  "); // con espacios
        assertNotNull(response);
        assertTrue(response.isAvailable());
        assertEquals("Correo disponible.", response.getMessage());
        verify(mockRepo, times(1)).existsByCorreoIgnoreCase("correo@test.com");
    }

    @Test
    @DisplayName("Test para actualizar un usuario, debe actualizar los campos")
    void actualizarUsuarioNoRol_deberiaActualizarCampos() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setCorreo("admin@mail.com");
        actor.setRol(Rol.ADMIN);

        // Usuario existente (el que se modifica)
        Usuario existente = new Usuario();
        existente.setUsuarioId(2L);
        existente.setUsername("oldUser");
        existente.setCorreo("old@mail.com");
        existente.setRol(Rol.VENDEDOR);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(usuarioRepository.findByUsernameIgnoreCase("adminUser"))
                .thenReturn(Optional.of(actor));

        doReturn(existente).when(serviceSpy).obtenerUsuarioRealPorId(2L);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .build();
        });
        ReflectionTestUtils.setField(serviceSpy, "usuarioMapper", mockMapper);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("NuevoNombre");
        request.setApellido("NuevoApellido");
        request.setCorreo("nuevo@correo.com");
        request.setUsername("nuevoUser");

        UsuarioResponse response = serviceSpy.actualizarUsuarioNoRol(2L, request);

        assertEquals("NuevoNombre", response.getNombre());
        assertEquals("NuevoApellido", response.getApellido());
        assertEquals("nuevo@correo.com", response.getCorreo());
        assertEquals("nuevoUser", response.getUsername());
    }

    @Test
    @DisplayName("Test para actualizar un usuario, username nulo,debe lanzar excepcion")
    void actualizarUsuarioNoRol_userNameNulo_deberiaLanzarExcepcion() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setCorreo("admin@mail.com");
        actor.setRol(Rol.ADMIN);

        // Usuario existente (el que se modifica)
        Usuario existente = new Usuario();
        existente.setUsuarioId(2L);
        existente.setUsername("oldUser");
        existente.setCorreo("old@mail.com");
        existente.setRol(Rol.VENDEDOR);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(usuarioRepository.findByUsernameIgnoreCase("adminUser"))
                .thenReturn(Optional.of(actor));

        doReturn(existente).when(serviceSpy).obtenerUsuarioRealPorId(2L);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .build();
        });
        ReflectionTestUtils.setField(serviceSpy, "usuarioMapper", mockMapper);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("NuevoNombre");
        request.setApellido("NuevoApellido");
        request.setCorreo("nuevo@correo.com");
        request.setUsername(null);

        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceSpy.actualizarUsuarioNoRol(2L, request)
        );

        assertEquals("El username no puede estar vacío.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar un usuario, username vacio, debe lanzar excepcion")
    void actualizarUsuarioNoRol_userNameVacio_deberiaLanzarExcepcion() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setCorreo("admin@mail.com");
        actor.setRol(Rol.ADMIN);

        // Usuario existente (el que se modifica)
        Usuario existente = new Usuario();
        existente.setUsuarioId(2L);
        existente.setUsername("oldUser");
        existente.setCorreo("old@mail.com");
        existente.setRol(Rol.VENDEDOR);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(usuarioRepository.findByUsernameIgnoreCase("adminUser"))
                .thenReturn(Optional.of(actor));

        doReturn(existente).when(serviceSpy).obtenerUsuarioRealPorId(2L);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .build();
        });
        ReflectionTestUtils.setField(serviceSpy, "usuarioMapper", mockMapper);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("NuevoNombre");
        request.setApellido("NuevoApellido");
        request.setCorreo("nuevo@correo.com");
        request.setUsername(" ");

        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serviceSpy.actualizarUsuarioNoRol(2L, request)
        );

        assertEquals("El username no puede estar vacío.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar un usuario sin cambiar username ni correo, debe actualizar los campos")
    void actualizarUsuarioNoRol_noUsernameNoCorreo_deberiaActualizarCampos() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Usuario autenticado (actor)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setCorreo("admin@mail.com");
        actor.setRol(Rol.ADMIN);

        // Usuario existente (el que se modifica)
        Usuario existente = new Usuario();
        existente.setUsuarioId(2L);
        existente.setUsername("oldUser");
        existente.setCorreo("old@mail.com");
        existente.setRol(Rol.VENDEDOR);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(usuarioRepository.findByUsernameIgnoreCase("adminUser"))
                .thenReturn(Optional.of(actor));

        doReturn(existente).when(serviceSpy).obtenerUsuarioRealPorId(2L);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .nombre(u.getNombre())
                    .apellido(u.getApellido())
                    .correo(u.getCorreo())
                    .username(u.getUsername())
                    .build();
        });
        ReflectionTestUtils.setField(serviceSpy, "usuarioMapper", mockMapper);

        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("NuevoNombre");
        request.setApellido("NuevoApellido");
        request.setCorreo("old@mail.com");
        request.setUsername("oldUser");

        UsuarioResponse response = serviceSpy.actualizarUsuarioNoRol(2L, request);

        assertEquals("NuevoNombre", response.getNombre());
        assertEquals("NuevoApellido", response.getApellido());
        assertEquals("old@mail.com", response.getCorreo());
        assertEquals("oldUser", response.getUsername());
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, no se puede actualizar el superadmin")
    void actualizarRolDeUsuario_noPuedeAsignarSuperAdmin() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor autenticado
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setRol(Rol.ADMIN);

        // Crear el target
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.VENDEDOR);

        // Crear request con SUPER_ADMIN
        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.SUPER_ADMIN);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar manualmente el mock del repositorio en el spy
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks para devolver el actor y el target
        when(usuarioRepository.findByUsernameIgnoreCase("adminUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("adminUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("El rol SUPER_ADMIN no puede ser asignado.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, no se puede asignar su propio rol")
    void actualizarRolDeUsuario_noPuedeAsignarSuPropioRol_lanzaExcepcion() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor autenticado
        Usuario actor = new Usuario();
        actor.setUsuarioId(2L);
        actor.setUsername("adminUser");
        actor.setRol(Rol.VENDEDOR);

        // Crear el target
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.VENDEDOR);

        // Crear request con SUPER_ADMIN
        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.SUPER_ADMIN);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar manualmente el mock del repositorio en el spy
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks para devolver el actor y el target
        when(usuarioRepository.findByUsernameIgnoreCase("adminUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("adminUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("No puedes cambiar tu propio rol.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, un ADMIN solo puede gestionar otros VENDEDORES")
    void actualizarRolDeUsuario_adminSoloPuedeAsingarRolDeVendedores_lanzaExcepcion() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor autenticado
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setRol(Rol.ADMIN);

        // Crear el target
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.ADMIN);

        // Crear request con ADMIN
        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.ADMIN);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar manualmente el mock del repositorio en el spy
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks para devolver el actor y el target
        when(usuarioRepository.findByUsernameIgnoreCase("adminUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("adminUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("Los administradores solo pueden gestionar a vendedores.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, no se puede asignar rol de un SUPER_ADMIN")
    void actualizarRolDeUsuario_noSePuedeActualizarSUPERADMIN_lanzaExcepcion() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor autenticado
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("superAdminUser");
        actor.setRol(Rol.SUPER_ADMIN);

        // Crear el target
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.SUPER_ADMIN);

        // Crear request con ADMIN
        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.ADMIN);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("superAdminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar manualmente el mock del repositorio en el spy
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks para devolver el actor y el target
        when(usuarioRepository.findByUsernameIgnoreCase("superAdminUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("superAdminUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("El usuario SUPER_ADMIN no puede ser modificado.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, admin solo gestiona a los vendedores")
    void actualizarRolDeUsuario_adminSoloGestionaVendedores() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor autenticado
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setRol(Rol.ADMIN);

        // Crear el target con rol distinto a VENDEDOR (por ejemplo, ADMIN)
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.ADMIN);

        // Crear request
        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.VENDEDOR);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar el mock del repositorio en el spy
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks
        when(usuarioRepository.findByUsernameIgnoreCase("adminUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("adminUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("Los administradores solo pueden gestionar a vendedores.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, admin solo gestiona a los vendedores")
    void actualizarRolDeUsuario_adminSolo21312GestionaVendedores() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor autenticado
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setRol(Rol.ADMIN);

        // Crear el target con rol distinto a VENDEDOR (por ejemplo, ADMIN)
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.ADMIN);

        // Crear request
        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.VENDEDOR);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar el mock del repositorio en el spy
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks
        when(usuarioRepository.findByUsernameIgnoreCase("adminUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("adminUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("Los administradores solo pueden gestionar a vendedores.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para actualizar el rol de un usuario, no se puede modificar el superadmin")
    void actualizarRolDeUsuario_noPuedeModificarSuperAdmin() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor autenticado
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("adminUser");
        actor.setRol(Rol.ADMIN);

        // Crear el target con rol SUPER_ADMIN
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.SUPER_ADMIN);

        // Crear request
        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.VENDEDOR);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("adminUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar el mock del repositorio en el spy
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks
        when(usuarioRepository.findByUsernameIgnoreCase("adminUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("adminUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("El usuario SUPER_ADMIN no puede ser modificado.", exception.getMessage());
    }

    // Test complementario, actualizacion de rol, no se puede modificar el superadmin
    @Test
    @DisplayName("Test para actualizar el rol de un usuario, no se puede modificar el superadmin, segunda excepcion")
    void actualizarRolDeUsuario_noPuedeModificarSuperAdmin2() {
        UsuarioService serviceSpy = spy(serviceUser);

        // Crear el actor (puede ser SUPER_ADMIN o cualquier rol que no sea ADMIN)
        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("superUser");
        actor.setRol(Rol.SUPER_ADMIN); // Importante: no es ADMIN para evitar otra validación

        // Crear el target que es SUPER_ADMIN
        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setRol(Rol.SUPER_ADMIN);

        // Crear request para intentar cambiar el rol

        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.ADMIN);

        // Simular autenticación
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("superUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Inyectar manualmente el mock del repositorio
        ReflectionTestUtils.setField(serviceSpy, "usuarioRepository", usuarioRepository);

        // Configurar mocks para devolver actor y target
        when(usuarioRepository.findByUsernameIgnoreCase("superUser")).thenReturn(Optional.of(actor));
        when(usuarioRepository.findByCorreo("superUser")).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));

        // Act + Assert
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> serviceSpy.actualizarRolDeUsuario(request)
        );

        assertEquals("El usuario SUPER_ADMIN no puede ser modificado.", exception.getMessage());
    }

    @Test
    @DisplayName("Test para obtener los usuarios filtrados segun filtro de pagina")
    void getUsuariosFiltradosPaginados_deberiaRetornarUsuariosPaginadosCorrectamente() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        // Datos simulados
        Usuario u1 = new Usuario();
        u1.setUsuarioId(1L);
        u1.setNombre("Juan");
        u1.setApellido("Pérez");
        u1.setCorreo("juan@test.com");
        u1.setActivo(true);

        Usuario u2 = new Usuario();
        u2.setUsuarioId(2L);
        u2.setNombre("Ana");
        u2.setApellido("Gómez");
        u2.setCorreo("ana@test.com");
        u2.setActivo(true);

        List<Usuario> listaUsuarios = List.of(u1, u2);
        Page<Usuario> pageUsuarios = new PageImpl<>(listaUsuarios);
        when(mockRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageUsuarios);

        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            UsuarioResponse r = new UsuarioResponse();
            r.setUsuarioId(u.getUsuarioId());
            r.setNombre(u.getNombre());
            r.setApellido(u.getApellido());
            r.setCorreo(u.getCorreo());
            r.setActivo(u.getActivo());
            return r;
        });

        // DTO de filtro vacío
        UsuarioFilterDTO filtro = new UsuarioFilterDTO();
        PaginacionResponse<UsuarioResponse> expectedResponse = new PaginacionResponse<>();

        UsuarioResponse ur1 = new UsuarioResponse();
        ur1.setUsuarioId(1L);
        ur1.setNombre("Juan");
        ur1.setApellido("Pérez");
        ur1.setCorreo("juan@test.com");
        ur1.setActivo(true);

        UsuarioResponse ur2 = new UsuarioResponse();
        ur2.setUsuarioId(2L);
        ur2.setNombre("Ana");
        ur2.setApellido("Gómez");
        ur2.setCorreo("ana@test.com");
        ur2.setActivo(true);

        expectedResponse.setContent(List.of(ur1, ur2));
        expectedResponse.setPageNumber(0);
        expectedResponse.setPageSize(2);
        expectedResponse.setTotalElements(2L);
        expectedResponse.setTotalPages(1);
        expectedResponse.setFirst(true);
        expectedResponse.setLast(true);
        expectedResponse.setEmpty(false);

        try (MockedStatic<PaginacionMapper> mocked = mockStatic(PaginacionMapper.class)) {
            mocked.when(() -> PaginacionMapper.mapToResponse(any(Page.class)))
                    .thenReturn(expectedResponse);

            PaginacionResponse<UsuarioResponse> result = service.getUsuariosFiltradosPaginados(0, 2, "nombre", filtro);
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals("Juan", result.getContent().get(0).getNombre());
            assertEquals("Ana", result.getContent().get(1).getNombre());
            verify(mockRepo).findAll(any(Specification.class), any(Pageable.class));

            // Verificamos que se haya invocado el mapper estático una vez
            mocked.verify(() -> PaginacionMapper.mapToResponse(any(Page.class)), times(1));
        }
    }

    @Test
    @DisplayName("Test para registar a un usuario, debe lanzar excepcion si existe (caso adicional)")
    void registrarUsuario_deberiaLanzarException_siUsernameYaExiste() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker realChecker = new AvailabilityChecker();
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, realChecker);
        String usernameExistente = "usuario1";
        // Simulamos que el username ya existe
        when(mockRepo.existsByUsernameIgnoreCase(usernameExistente)).thenReturn(true);
        // Creamos request con username repetido
        UsuarioRequest request = new UsuarioRequest();
        request.setUsername(usernameExistente);
        request.setNombre("Nombre");
        request.setApellido("Apellido");
        request.setCorreo("correo@ejemplo.com");
        request.setPassword("123456");
        request.setRol(Rol.VENDEDOR);
        // Verificamos que al registrar usuario lance la excepción
        ExistsRegisterException ex = assertThrows(ExistsRegisterException.class, () -> {
            service.registrarUsuario(request);
        });
        assertEquals("El username " + usernameExistente + " ya está registrado.", ex.getMessage());
        // Verificamos que el repo fue consultado
        verify(mockRepo).existsByUsernameIgnoreCase(usernameExistente);
    }

    @Test
    @DisplayName("Test para registar a un usuario, debe lanzar excepcion si existe (caso adicional)")
    void registrarUsuario_deberiaLanzarException_siCorreoYaExiste() {
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker realChecker = new AvailabilityChecker();
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, realChecker);
        String correoExistente = "correo1";
        // Simulamos que el correo ya existe
        when(mockRepo.existsByCorreoIgnoreCase(correoExistente)).thenReturn(true);
        // Creamos request con username repetido
        UsuarioRequest request = new UsuarioRequest();
        request.setUsername("username");
        request.setNombre("Nombre");
        request.setApellido("Apellido");
        request.setCorreo(correoExistente);
        request.setPassword("123456");
        request.setRol(Rol.VENDEDOR);
        // Verificamos que al registrar usuario lance la excepción
        ExistsRegisterException ex = assertThrows(ExistsRegisterException.class, () -> {
            service.registrarUsuario(request);
        });
        // Verificamos que el repo fue consultado
        assertEquals("El correo " + correoExistente + " ya está registrado.", ex.getMessage());
        // Verificamos que el repo fue consultado
        verify(mockRepo).existsByCorreoIgnoreCase(correoExistente);
    }

    @Test
    @DisplayName("Test SUPER_ADMIN actualiza rol de ADMIN a VENDEDOR exitosamente")
    void actualizarRolDeUsuario_SuperAdminActualizaAdmin_deberiaExitoso() {
        // Arrange
        UsuarioRepository mockRepo = mock(UsuarioRepository.class);
        UsuarioMapper mockMapper = mock(UsuarioMapper.class);
        AvailabilityChecker mockChecker = mock(AvailabilityChecker.class);
        UsuarioService service = new UsuarioService(mockRepo, mockMapper, mockChecker);

        Usuario actor = new Usuario();
        actor.setUsuarioId(1L);
        actor.setUsername("superadmin");
        actor.setRol(Rol.SUPER_ADMIN);

        Usuario target = new Usuario();
        target.setUsuarioId(2L);
        target.setUsername("admin_user");
        target.setRol(Rol.ADMIN);

        UsuarioUpdateRol request = new UsuarioUpdateRol(2L, Rol.VENDEDOR);
        Authentication auth = new UsernamePasswordAuthenticationToken("superadmin", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Configurar Mocks
        when(mockRepo.findByUsernameIgnoreCase("superadmin")).thenReturn(Optional.of(actor));
        when(mockRepo.findById(2L)).thenReturn(Optional.of(target));
        when(mockRepo.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mockMapper.toResponse(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return UsuarioResponse.builder()
                    .usuarioId(u.getUsuarioId())
                    .username(u.getUsername())
                    .rol(u.getRol())
                    .build();
        });

        // Act
        UsuarioResponse response = service.actualizarRolDeUsuario(request);

        // Assert
        assertNotNull(response);
        assertEquals(Rol.VENDEDOR, response.getRol());

        verify(mockRepo, times(1)).save(any(Usuario.class));
        SecurityContextHolder.clearContext();
    }
}
