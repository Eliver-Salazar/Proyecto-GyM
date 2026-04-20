package com.gym.controller;

import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Membresia;
import com.gym.domain.Pago;
import com.gym.domain.Usuario;
import com.gym.service.UsuarioService;
import com.gym.service.oracle.OracleBitacoraService;
import com.gym.service.oracle.OraclePackageException;
import com.gym.service.oracle.dto.BitacoraAccesoOracleDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final UsuarioService usuarioService;
    private final OracleBitacoraService oracleBitacoraService;

    public PerfilController(UsuarioRepository usuarioRepository,
                            MembresiaRepository membresiaRepository,
                            PagoRepository pagoRepository,
                            UsuarioService usuarioService,
                            OracleBitacoraService oracleBitacoraService) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioService = usuarioService;
        this.oracleBitacoraService = oracleBitacoraService;
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

        List<BitacoraAccesoOracleDto> accesosRecientes;
        int accesosDenegados;

        try {
            accesosRecientes = oracleBitacoraService.listarAccesosPorUsuario(usuario.getIdUsuario())
                    .stream()
                    .limit(5)
                    .toList();

            accesosDenegados = oracleBitacoraService.contarAccesosDenegadosUsuario(usuario.getIdUsuario());

        } catch (OraclePackageException ex) {
            accesosRecientes = List.of();
            accesosDenegados = 0;
            model.addAttribute("advertenciaBitacora", ex.getMessage());
        }

        BigDecimal totalPagado = pagosRecientes.stream()
                .map(p -> p.getMonto() != null ? p.getMonto() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long diasRestantes = calcularDiasRestantes(membresiaActual);
        String estadoVigencia = determinarEstadoVigencia(membresiaActual);
        String accesoGlobalTexto = obtenerAccesoGlobalTexto(membresiaActual);

        model.addAttribute("membresiaActual", membresiaActual);
        model.addAttribute("pagosRecientes", pagosRecientes);
        model.addAttribute("accesosRecientes", accesosRecientes);
        model.addAttribute("accesosDenegados", accesosDenegados);
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

        String cedulaNormalizada = limpiarCedula(formulario.getCedula());

        if (cedulaNormalizada == null || !cedulaNormalizada.matches("\\d{10,20}")) {
            redirectAttributes.addFlashAttribute("error", "La cédula debe contener únicamente números y tener entre 10 y 20 dígitos.");
            return "redirect:/perfil/editar";
        }

        usuarioActual.setNombre(formulario.getNombre());
        usuarioActual.setApellido(formulario.getApellido());
        usuarioActual.setCorreo(formulario.getCorreo());
        usuarioActual.setTelefono(formulario.getTelefono());
        usuarioActual.setCedula(cedulaNormalizada);
        usuarioActual.setFechaNacimiento(formulario.getFechaNacimiento());
        usuarioActual.setContactoEmergencia(formulario.getContactoEmergencia());
        usuarioActual.setTelefonoEmergencia(formulario.getTelefonoEmergencia());
        usuarioActual.setCondicionMedica(formulario.getCondicionMedica());

        try {
            usuarioService.guardarUsuario(usuarioActual);
        } catch (DataIntegrityViolationException ex) {
            String mensaje = "No se pudo actualizar el perfil por una restricción de base de datos.";

            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            if (detalle != null) {
                String detalleUpper = detalle.toUpperCase();

                if (detalleUpper.contains("USUARIOS_CEDULA_NUM_CHK")) {
                    mensaje = "La cédula debe contener únicamente números y tener entre 10 y 20 dígitos.";
                } else if (detalleUpper.contains("CORREO")) {
                    mensaje = "Ya existe otro usuario registrado con ese correo.";
                } else if (detalleUpper.contains("CEDULA")) {
                    mensaje = "Ya existe otro usuario registrado con esa cédula.";
                }
            }

            redirectAttributes.addFlashAttribute("error", mensaje);
            return "redirect:/perfil/editar";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado al actualizar el perfil.");
            return "redirect:/perfil/editar";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/perfil";
    }

    private long calcularDiasRestantes(Membresia membresiaActual) {
        if (membresiaActual == null || membresiaActual.getFechaVencimiento() == null) {
            return 0;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaVencimiento = convertirFechaALocalDate(membresiaActual.getFechaVencimiento());

        if (fechaVencimiento.isBefore(hoy)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(hoy, fechaVencimiento) + 1;
    }

    private String determinarEstadoVigencia(Membresia membresiaActual) {
        if (membresiaActual == null) {
            return "SIN MEMBRESÍA";
        }

        if (membresiaActual.getFechaVencimiento() != null) {
            LocalDate hoy = LocalDate.now();
            LocalDate fechaVencimiento = convertirFechaALocalDate(membresiaActual.getFechaVencimiento());

            if (fechaVencimiento.isBefore(hoy)) {
                return "VENCIDA";
            }
        }

        if (membresiaActual.getEstadoMembresia() != null
                && membresiaActual.getEstadoMembresia().getNombreEstado() != null
                && !membresiaActual.getEstadoMembresia().getNombreEstado().isBlank()) {
            return membresiaActual.getEstadoMembresia().getNombreEstado().trim().toUpperCase();
        }

        return "ACTIVA";
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

    private LocalDate convertirFechaALocalDate(Date fecha) {
        if (fecha == null) {
            return null;
        }

        if (fecha instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return fecha.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private String limpiarCedula(String cedula) {
        if (cedula == null) {
            return null;
        }
        return cedula.replaceAll("[^0-9]", "").trim();
    }
}