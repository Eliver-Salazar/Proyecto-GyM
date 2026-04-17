package com.gym.controller;

import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Membresia;
import com.gym.domain.Usuario;
import com.gym.service.UsuarioService;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final UsuarioService usuarioService;

    public PerfilController(UsuarioRepository usuarioRepository,
                            MembresiaRepository membresiaRepository,
                            UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil")
    public String perfil(Model model, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(principal.getName());
        if (usuarioOpt.isEmpty()) {
            return "redirect:/login?error";
        }

        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuarioPerfil", usuario);

        List<Membresia> membresias = membresiaRepository.findByUsuario(usuario);
        Membresia membresiaActual = membresias.stream()
                .max(Comparator.comparing(
                        Membresia::getFechaVencimiento,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .orElse(null);

        model.addAttribute("membresiaActual", membresiaActual);

        return "perfil";
    }

    @GetMapping("/perfil/editar")
    public String editarPerfil(Model model, Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(principal.getName());
        if (usuarioOpt.isEmpty()) {
            return "redirect:/perfil?error";
        }

        model.addAttribute("u", usuarioOpt.get());
        return "perfil-editar";
    }

    @PostMapping("/perfil/editar")
    public String guardarPerfil(@ModelAttribute("u") Usuario formulario,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(principal.getName());
        if (usuarioOpt.isEmpty()) {
            return "redirect:/perfil?error";
        }

        Usuario usuarioActual = usuarioOpt.get();

        if (formulario.getNombre() == null || formulario.getNombre().isBlank()
                || formulario.getApellido() == null || formulario.getApellido().isBlank()
                || formulario.getCorreo() == null || formulario.getCorreo().isBlank()
                || formulario.getTelefono() == null || formulario.getTelefono().isBlank()
                || formulario.getCedula() == null || formulario.getCedula().isBlank()
                || formulario.getFechaNacimiento() == null
                || formulario.getTelefonoEmergencia() == null || formulario.getTelefonoEmergencia().isBlank()) {

            redirectAttributes.addFlashAttribute("error", "Debes completar todos los campos obligatorios del perfil.");
            return "redirect:/perfil/editar";
        }

        usuarioActual.setNombre(formulario.getNombre());
        usuarioActual.setApellido(formulario.getApellido());
        usuarioActual.setCorreo(formulario.getCorreo());
        usuarioActual.setTelefono(formulario.getTelefono());
        usuarioActual.setCedula(formulario.getCedula());
        usuarioActual.setFechaNacimiento(formulario.getFechaNacimiento());
        usuarioActual.setContactoEmergencia(formulario.getContactoEmergencia());
        usuarioActual.setTelefonoEmergencia(formulario.getTelefonoEmergencia());
        usuarioActual.setCondicionMedica(formulario.getCondicionMedica());

        usuarioService.guardarUsuario(usuarioActual);

        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/perfil";
    }
}