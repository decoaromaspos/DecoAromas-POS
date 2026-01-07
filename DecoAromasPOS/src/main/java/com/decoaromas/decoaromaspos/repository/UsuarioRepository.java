package com.decoaromas.decoaromaspos.repository;

import com.decoaromas.decoaromaspos.enums.Rol;
import com.decoaromas.decoaromaspos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findByActivoTrue();
    List<Usuario> findByActivoFalse();
    List<Usuario> findByRolIsNotLike(Rol rol);

    // Buscar por nombre parcial (case insensitive)
    List<Usuario> findByNombreContainingIgnoreCase(String nombre);

    // Buscar por nombre parcial y apellido parcial
    List<Usuario> findByNombreContainingIgnoreCaseAndApellidoContainingIgnoreCase(String nombre, String apellido);

    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCaseAndUsuarioIdNot(String correo, Long usuarioId);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseAndUsuarioIdNot(String username, Long idExcluir);
}
