package com.decoaromas.decoaromaspos.config;

import com.decoaromas.decoaromaspos.enums.Rol;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Esta clase se ejecuta una vez al iniciar la aplicación.
 * Su propósito es verificar si el usuario SUPER_ADMIN existe y, si no, crearlo.
 */
//@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Inyecta las credenciales desde application.properties (que a su vez las leerá del entorno)
    @Value("${app.superadmin.username}")
    private String superAdminUsername;

    @Value("${app.superadmin.password}")
    private String superAdminPassword;

    @Value("${app.superadmin.email}")
    private String superAdminEmail;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Busca al usuario por su nombre de usuario (que es único)
        if (usuarioRepository.findByUsername(superAdminUsername).isEmpty()) {
            logger.info("El usuario SUPER_ADMIN no existe. Creando...");

            Usuario superAdmin = Usuario.builder()
                    .nombre("Super")
                    .apellido("Admin")
                    .correo(superAdminEmail)
                    .username(superAdminUsername)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .rol(Rol.SUPER_ADMIN)
                    .activo(true)
                    .build();

            usuarioRepository.save(superAdmin);
            logger.info("¡Usuario SUPER_ADMIN creado exitosamente!");
        } else {
            logger.info("ℹEl usuario SUPER_ADMIN ya existe!. No se realizaron cambios.");
        }
    }
}