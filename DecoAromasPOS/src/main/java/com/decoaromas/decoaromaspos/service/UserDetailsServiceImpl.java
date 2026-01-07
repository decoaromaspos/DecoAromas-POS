package com.decoaromas.decoaromaspos.service;

import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.enums.Rol;
import com.decoaromas.decoaromaspos.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrCorreo) throws UsernameNotFoundException {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsernameIgnoreCase(usernameOrCorreo);
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByCorreo(usernameOrCorreo);
        }

        Usuario usuario = usuarioOpt.orElseThrow(() ->
                new UsernameNotFoundException("Usuario no encontrado con identificador: " + usernameOrCorreo)
        );

        // Obtener nombre del rol desde el enum Rol
        String rawRole;
        Rol rolEnum = usuario.getRol();
        if (rolEnum == null) {
            rawRole = "USER";
        } else {
            rawRole = rolEnum.name(); // Devuelve ADMIN, TRABAJADOR, CLIENTE, etc.
        }

        // Normalizar y asegurar prefijo ROLE_
        String normalizedRole = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole));

        boolean enabled = Boolean.TRUE.equals(usuario.getActivo());

        String usernameToUse = usuario.getUsername() != null && !usuario.getUsername().isBlank()
                ? usuario.getUsername()
                : usuario.getCorreo();

        return User.builder()
                .username(usernameToUse)
                .password(usuario.getPassword())
                .authorities(authorities)
                .disabled(!enabled)
                .build();
    }
}
