package com.gym.service;

import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Rol;
import com.gym.domain.Usuario;
import java.util.Collections;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        if (usuario.getContrasenia() == null || usuario.getContrasenia().isBlank()) {
            throw new UsernameNotFoundException("El usuario no tiene contraseña válida registrada: " + correo);
        }

        Rol rol = usuario.getRol();
        if (rol == null || rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new UsernameNotFoundException("El usuario no tiene un rol válido asignado: " + correo);
        }

        String authority = "ROLE_" + rol.getNombre().trim().toUpperCase(Locale.ROOT);

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getContrasenia())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(authority)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}