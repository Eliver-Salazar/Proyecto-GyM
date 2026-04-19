package com.gym.controller;

import com.gym.Repository.BitacoraAccesoRepository;
import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.BitacoraAcceso;
import com.gym.domain.Membresia;
import com.gym.domain.Pago;
import com.gym.domain.Usuario;
import com.gym.service.UsuarioService;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Comparator;
import java.util.Date;
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
    private final PagoRepository pagoRepository;
    private final BitacoraAccesoRepository bitacoraAccesoRepository;
    private final UsuarioService usuarioService;

    public PerfilController(UsuarioRepository usuarioRepository,
                            MembresiaRepository membresiaRepository,
                            PagoRepository pagoRepository,
                            BitacoraAccesoRepository bitacoraAccesoRepository,
                            UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.pagoRepository = pagoRepository;
        this.bitacoraAccesoRepository = bitacoraAccesoRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping({"/perfil", "/usuario/perfil"})
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

        Membresia membresiaActual = membresiaRepository
                .findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElse(null);

        List<Pago> pagosRecientes = pagoRepository.findByUsuarioIdUsuario(usuario.getIdUsuario()).stream()
                .sorted(Comparator.comparing(Pago::getFechaPago, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .toList();

        List<BitacoraAcceso> accesosRecientes = bitacoraAccesoRepository
                .findByUsuarioIdUsuarioOrderByFechaHoraDesc(usuario.getIdUsuario())
                .stream()
                .limit(5)
                .toList();

        BigDecimal totalPagado = pagosRecientes.stream()
                .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long diasRestantes = calcularDiasRestantes(membresiaActual);

        String estadoVigencia = determinarEstadoVigencia(membresiaActual, diasRestantes);
        String accesoGlobalTexto = obtenerAccesoGlobalTexto(membresiaActual);

        model.addAttribute("membresiaActual", membresiaActual);
        model.addAttribute("pagosRecientes", pagosRecientes);
        model.addAttribute("accesosRecientes", accesosRecientes);
        model.addAttribute("totalPagadoReciente", totalPagado);
        model.addAttribute("diasRestantes", diasRestantes);
        model.addAttribute("estadoVigencia", estadoVigencia);
        model.addAttribute("accesoGlobalTexto", accesoGlobalTexto);

        return "perfil";
    }

    @GetMapping({"/perfil/editar", "/usuario/perfil/editar"})
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

    @PostMapping({"/perfil/editar", "/usuario/perfil/editar"})
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

    private long calcularDiasRestantes(Membresia membresiaActual) {
        if (membresiaActual == null || membresiaActual.getFechaVencimiento() == null) {
            return 0;
        }

        long diferencia = membresiaActual.getFechaVencimiento().getTime() - new Date().getTime();
        long dias = diferencia / (1000L * 60L * 60L * 24L);

        return Math.max(dias, 0);
    }

    private String determinarEstadoVigencia(Membresia membresiaActual, long diasRestantes) {
        if (membresiaActual == null) {
            return "SIN MEMBRESÍA";
        }

        if (membresiaActual.getEstadoMembresia() != null
                && membresiaActual.getEstadoMembresia().getNombreEstado() != null
                && !membresiaActual.getEstadoMembresia().getNombreEstado().isBlank()) {
            return membresiaActual.getEstadoMembresia().getNombreEstado();
        }

        return diasRestantes > 0 ? "ACTIVA" : "VENCIDA";
    }

    private String obtenerAccesoGlobalTexto(Membresia membresiaActual) {
        if (membresiaActual == null
                || membresiaActual.getTipoMembresia() == null
                || membresiaActual.getTipoMembresia().getAccesoGlobal() == null) {
            return "No definido";
        }

        return "S".equalsIgnoreCase(membresiaActual.getTipoMembresia().getAccesoGlobal())
                ? "Sí"
                : "No";
    }
}