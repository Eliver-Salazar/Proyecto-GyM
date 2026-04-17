package com.gym.service;

import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Usuario;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Override
    public Usuario guardarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }

        if (usuario.getCorreo() != null) {
            usuario.setCorreo(usuario.getCorreo().trim().toLowerCase());
        }

        if (usuario.getCedula() != null) {
            usuario.setCedula(usuario.getCedula().trim());
        }

        if (usuario.getNombre() != null) {
            usuario.setNombre(usuario.getNombre().trim());
        }

        if (usuario.getApellido() != null) {
            usuario.setApellido(usuario.getApellido().trim());
        }

        if (usuario.getTelefono() != null) {
            usuario.setTelefono(usuario.getTelefono().trim());
        }

        if (usuario.getContactoEmergencia() != null) {
            usuario.setContactoEmergencia(usuario.getContactoEmergencia().trim());
        }

        if (usuario.getTelefonoEmergencia() != null) {
            usuario.setTelefonoEmergencia(usuario.getTelefonoEmergencia().trim());
        }

        if (usuario.getCondicionMedica() != null) {
            usuario.setCondicionMedica(usuario.getCondicionMedica().trim());
        }

        String contrasenia = usuario.getContrasenia();
        if (contrasenia != null && !contrasenia.isBlank()) {
            boolean yaEsHash =
                    contrasenia.startsWith("$2a$")
                    || contrasenia.startsWith("$2b$")
                    || contrasenia.startsWith("$2y$");

            if (!yaEsHash) {
                usuario.setContrasenia(passwordEncoder.encode(contrasenia));
            }
        }

        return usuarioRepository.save(usuario);
    }

    @Override
    public void eliminarUsuario(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El id del usuario no puede ser nulo.");
        }
        usuarioRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase()).orElse(null);
    }
}