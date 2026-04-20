package com.gym.controller;

import com.gym.Repository.BitacoraAccesoRepository;
import com.gym.Repository.EmpleadoRepository;
import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.BitacoraAcceso;
import com.gym.domain.Empleado;
import com.gym.domain.Membresia;
import com.gym.domain.Pago;
import com.gym.domain.Usuario;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PersonalController {

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final PagoRepository pagoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final BitacoraAccesoRepository bitacoraAccesoRepository;

    public PersonalController(UsuarioRepository usuarioRepository,
                              MembresiaRepository membresiaRepository,
                              PagoRepository pagoRepository,
                              EmpleadoRepository empleadoRepository,
                              BitacoraAccesoRepository bitacoraAccesoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.pagoRepository = pagoRepository;
        this.empleadoRepository = empleadoRepository;
        this.bitacoraAccesoRepository = bitacoraAccesoRepository;
    }

    @GetMapping({"/personal", "/personal/", "/personal/panel"})
    public String panel(Model model, Principal principal) {
        Usuario usuarioActual = null;
        Empleado empleadoActual = null;

        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(principal.getName());
            if (usuarioOpt.isPresent()) {
                usuarioActual = usuarioOpt.get();
                empleadoActual = empleadoRepository
                        .findByUsuarioIdUsuario(usuarioActual.getIdUsuario())
                        .orElse(null);
            }
        }

        List<Usuario> usuarios = usuarioRepository.findAll();

        List<Usuario> socios = usuarios.stream()
                .filter(u -> u.getRol() != null && "CLIENTE".equalsIgnoreCase(u.getRol().getNombre()))
                .sorted(Comparator.comparing(Usuario::getIdUsuario, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();

        long totalSocios = socios.size();
        long totalMembresias = membresiaRepository.count();
        long totalPagos = pagoRepository.count();

        BigDecimal montoPagos = pagoRepository.findAll().stream()
                .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Usuario> sociosRecientes = socios.stream()
                .limit(5)
                .toList();

        model.addAttribute("usuarioOperativo", usuarioActual);
        model.addAttribute("empleadoActual", empleadoActual);
        model.addAttribute("totalSocios", totalSocios);
        model.addAttribute("totalMembresias", totalMembresias);
        model.addAttribute("totalPagos", totalPagos);
        model.addAttribute("montoPagos", montoPagos);
        model.addAttribute("sociosRecientes", sociosRecientes);

        return "personal/panel";
    }

    @GetMapping("/personal/socios")
    public String socios(Model model) {
        List<Usuario> socios = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && "CLIENTE".equalsIgnoreCase(u.getRol().getNombre()))
                .sorted(Comparator.comparing(Usuario::getIdUsuario, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();

        model.addAttribute("socios", socios);
        return "personal/socios";
    }

    @GetMapping("/personal/membresias")
    public String membresias(Model model) {
        List<Membresia> membresias = membresiaRepository.findAll().stream()
                .sorted(Comparator.comparing(Membresia::getIdMembresia, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();

        model.addAttribute("membresias", membresias);
        return "personal/membresias";
    }

    @GetMapping("/personal/pagos")
    public String pagos(Model model) {
        List<Pago> pagos = pagoRepository.findAll().stream()
                .sorted(Comparator.comparing(Pago::getIdPago, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();

        model.addAttribute("pagos", pagos);
        return "personal/pagos";
    }

    @GetMapping("/personal/accesos")
    public String accesos(Model model) {
        List<BitacoraAcceso> accesos = bitacoraAccesoRepository.findAll().stream()
                .sorted(Comparator.comparing(BitacoraAcceso::getFechaHora,
                        Comparator.nullsLast(java.util.Date::compareTo)).reversed()
                        .thenComparing(BitacoraAcceso::getIdAcceso,
                                Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();

        model.addAttribute("accesos", accesos);
        return "personal/accesos";
    }
}