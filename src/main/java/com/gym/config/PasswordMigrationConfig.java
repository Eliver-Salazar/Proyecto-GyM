package com.gym.config;

import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Usuario;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordMigrationConfig {

    /**
     * Migración opcional de contraseñas en texto plano a BCrypt.
     *
     * IMPORTANTE:
     * Esta migración queda desactivada por defecto para no ejecutar updates
     * masivos al arrancar la aplicación. En el modelo Oracle actual, la tabla
     * USUARIOS tiene FOTO_PERFIL como BLOB NOT NULL, por lo que un save() sobre
     * la entidad completa puede provocar errores si el campo no viene cargado
     * correctamente durante el arranque.
     *
     * Solo se activa si en application.properties se define:
     *
     * app.password-migration.enabled=true
     */
    @Bean
    @ConditionalOnProperty(
            name = "app.password-migration.enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public CommandLineRunner migratePlainPasswords(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            List<Usuario> usuarios = usuarioRepository.findAll();

            for (Usuario usuario : usuarios) {
                String contrasenia = usuario.getContrasenia();

                if (contrasenia == null || contrasenia.isBlank()) {
                    continue;
                }

                boolean yaEsHash =
                        contrasenia.startsWith("$2a$")
                        || contrasenia.startsWith("$2b$")
                        || contrasenia.startsWith("$2y$");

                if (!yaEsHash) {
                    usuario.setContrasenia(passwordEncoder.encode(contrasenia));
                    usuarioRepository.save(usuario);
                }
            }
        };
    }
}