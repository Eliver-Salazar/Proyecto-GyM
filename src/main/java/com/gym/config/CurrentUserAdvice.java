package com.gym.config;

import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Usuario;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class CurrentUserAdvice {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute
    public void addGlobals(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            model.addAttribute("authName", auth.getName());

            Set<String> roles = auth.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toSet());

            model.addAttribute("authRoles", roles);
            model.addAttribute("isAdmin", roles.contains("ROLE_ADMIN"));
            model.addAttribute("isPersonal", roles.contains("ROLE_PERSONAL"));
            model.addAttribute("isCliente", roles.contains("ROLE_CLIENTE"));

            Usuario usuario = usuarioRepository.findByCorreo(auth.getName()).orElse(null);
            model.addAttribute("usuarioActual", usuario);
        } else {
            model.addAttribute("usuarioActual", null);
            model.addAttribute("authName", null);
            model.addAttribute("authRoles", Set.of());
            model.addAttribute("isAdmin", false);
            model.addAttribute("isPersonal", false);
            model.addAttribute("isCliente", false);
        }
    }
}