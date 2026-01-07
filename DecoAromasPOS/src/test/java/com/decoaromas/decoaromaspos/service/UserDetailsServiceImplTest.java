package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.enums.Rol;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setUsername("testuser");
        usuario.setCorreo("test@example.com");
        usuario.setPassword("encryptedPass");
        usuario.setRol(Rol.ADMIN);
        usuario.setActivo(true);
    }

    @Test
    @DisplayName("Test para cargar un usuario por nombre, retorna los detalles del mismo..")
    void loadUserByUsername_shouldReturnUserDetails_whenUserFoundByUsername() {
        // Se le da lo siguiente
        when(usuarioRepository.findByUsernameIgnoreCase("testuser"))
                .thenReturn(Optional.of(usuario));

        // Cuando
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Entonces ocurre lo siguiente
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encryptedPass", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(usuarioRepository).findByUsernameIgnoreCase("testuser");
        verify(usuarioRepository, never()).findByCorreo(anyString());
    }

    @Test
    @DisplayName("Test para cargar un usuario por nombre, debe retonar los detalles cuando el correo es el correcto")
    void loadUserByUsername_shouldReturnUserDetails_whenUserFoundByCorreo() {
        // Se le da lo siguiente
        when(usuarioRepository.findByUsernameIgnoreCase("test@example.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("test@example.com"))
                .thenReturn(Optional.of(usuario));

        // Cuando el usuario
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Entonces sucede lo siguiente
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        verify(usuarioRepository).findByUsernameIgnoreCase("test@example.com");
        verify(usuarioRepository).findByCorreo("test@example.com");
    }

    @Test
    @DisplayName("Test para cargar un usuario, si no existe retornar excepcion")
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
        // Se le da
        when(usuarioRepository.findByUsernameIgnoreCase("unknown"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("unknown"))
                .thenReturn(Optional.empty());

        // Cuando / Entonces
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown")
        );

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
        verify(usuarioRepository).findByUsernameIgnoreCase("unknown");
        verify(usuarioRepository).findByCorreo("unknown");
    }
}
