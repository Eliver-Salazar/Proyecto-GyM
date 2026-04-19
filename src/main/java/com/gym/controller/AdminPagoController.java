package com.gym.controller;

import com.gym.Repository.CajaRepository;
import com.gym.Repository.EmpleadoRepository;
import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.MetodoPagoRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Caja;
import com.gym.domain.Empleado;
import com.gym.domain.Membresia;
import com.gym.domain.MetodoPago;
import com.gym.domain.Pago;
import com.gym.domain.Usuario;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminPagoController {

    private static final Pattern REFERENCIA_PATTERN =
            Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9 ]+$");

    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final CajaRepository cajaRepository;
    private final EmpleadoRepository empleadoRepository;

    public AdminPagoController(PagoRepository pagoRepository,
                               UsuarioRepository usuarioRepository,
                               MembresiaRepository membresiaRepository,
                               MetodoPagoRepository metodoPagoRepository,
                               CajaRepository cajaRepository,
                               EmpleadoRepository empleadoRepository) {
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.cajaRepository = cajaRepository;
        this.empleadoRepository = empleadoRepository;
    }

    @GetMapping("/admin/pagos")
    public String pagos(Model model) {
        prepararVistaPagos(model, nuevoPagoForm(), false);
        return "admin/pagos";
    }

    @GetMapping("/admin/pagos/editar/{id}")
    public String editarPago(@PathVariable("id") Long id,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        Optional<Pago> pagoOpt = pagoRepository.findById(id);

        if (pagoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El pago solicitado no existe.");
            return "redirect:/admin/pagos";
        }

        prepararVistaPagos(model, pagoOpt.get(), true);
        return "admin/pagos";
    }

    @PostMapping("/admin/pagos/guardar")
    public String guardarPago(@ModelAttribute("pagoForm") Pago pagoForm,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        boolean esEdicion = pagoForm.getIdPago() != null;
        inicializarRelacionesSiSonNull(pagoForm);

        if (pagoForm.getUsuario().getIdUsuario() == null
                || pagoForm.getMetodoPago().getIdMetodoPago() == null
                || pagoForm.getCaja().getIdCaja() == null
                || pagoForm.getEmpleado().getIdEmpleado() == null
                || pagoForm.getMonto() == null
                || pagoForm.getFechaPago() == null
                || pagoForm.getActivo() == null
                || pagoForm.getActivo().isBlank()) {

            model.addAttribute("error",
                    "Debes seleccionar cliente, método de pago, caja, empleado, monto, fecha y estado activo.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(pagoForm.getUsuario().getIdUsuario());
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("error", "El usuario seleccionado no existe.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        Usuario usuarioSeleccionado = usuarioOpt.get();

        if (usuarioSeleccionado.getRol() == null
                || usuarioSeleccionado.getRol().getNombre() == null
                || !"CLIENTE".equalsIgnoreCase(usuarioSeleccionado.getRol().getNombre())) {
            model.addAttribute("error", "Solo se pueden registrar pagos para usuarios con rol CLIENTE.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        Optional<Membresia> membresiaOpt = membresiaRepository.findByUsuarioIdUsuario(usuarioSeleccionado.getIdUsuario());
        if (membresiaOpt.isEmpty()) {
            model.addAttribute("error", "El cliente seleccionado no tiene una membresía registrada.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        Optional<MetodoPago> metodoOpt = metodoPagoRepository.findById(pagoForm.getMetodoPago().getIdMetodoPago());
        if (metodoOpt.isEmpty()) {
            model.addAttribute("error", "El método de pago seleccionado no existe.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        Optional<Caja> cajaOpt = cajaRepository.findById(pagoForm.getCaja().getIdCaja());
        if (cajaOpt.isEmpty()) {
            model.addAttribute("error", "La caja seleccionada no existe.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        Optional<Empleado> empleadoOpt = empleadoRepository.findById(pagoForm.getEmpleado().getIdEmpleado());
        if (empleadoOpt.isEmpty()) {
            model.addAttribute("error", "El empleado seleccionado no existe.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        if (pagoForm.getMonto().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "El monto del pago no puede ser negativo.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        String activoNormalizado = pagoForm.getActivo().trim().toUpperCase();
        pagoForm.setActivo(activoNormalizado);

        if (!"S".equals(activoNormalizado) && !"N".equals(activoNormalizado)) {
            model.addAttribute("error", "El campo activo debe ser 'S' o 'N'.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        if (pagoForm.getReferencia() != null && !pagoForm.getReferencia().isBlank()) {
            String referenciaNormalizada = pagoForm.getReferencia().trim().replaceAll("\\s+", " ").toUpperCase();
            pagoForm.setReferencia(referenciaNormalizada);

            if (referenciaNormalizada.length() > 50) {
                model.addAttribute("error", "La referencia no puede superar los 50 caracteres.");
                prepararVistaPagos(model, pagoForm, esEdicion);
                return "admin/pagos";
            }

            if (!REFERENCIA_PATTERN.matcher(referenciaNormalizada).matches()) {
                model.addAttribute("error", "La referencia solo puede contener letras, números y espacios.");
                prepararVistaPagos(model, pagoForm, esEdicion);
                return "admin/pagos";
            }
        }

        Pago pagoGuardar;

        if (esEdicion) {
            Optional<Pago> pagoOpt = pagoRepository.findById(pagoForm.getIdPago());
            if (pagoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el pago a editar.");
                return "redirect:/admin/pagos";
            }
            pagoGuardar = pagoOpt.get();
        } else {
            pagoGuardar = new Pago();
        }

        pagoGuardar.setUsuario(usuarioSeleccionado);
        pagoGuardar.setMembresia(membresiaOpt.get());
        pagoGuardar.setMetodoPago(metodoOpt.get());
        pagoGuardar.setCaja(cajaOpt.get());
        pagoGuardar.setEmpleado(empleadoOpt.get());
        pagoGuardar.setMonto(pagoForm.getMonto());
        pagoGuardar.setFechaPago(pagoForm.getFechaPago());
        pagoGuardar.setReferencia(
                pagoForm.getReferencia() == null || pagoForm.getReferencia().isBlank()
                        ? null
                        : pagoForm.getReferencia()
        );
        pagoGuardar.setActivo(activoNormalizado);

        try {
            pagoRepository.save(pagoGuardar);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            String detalleUpper = detalle != null ? detalle.toUpperCase() : "";
            String mensaje = "No se pudo guardar el pago por una restricción de base de datos.";

            if (detalleUpper.contains("ORA-02291")) {
                mensaje = "Una de las relaciones seleccionadas no existe en la base de datos.";
            } else if (detalleUpper.contains("ORA-02290")) {
                mensaje = "El pago no cumple una validación definida en la base de datos.";
            } else if (detalleUpper.contains("ACTIVO")) {
                mensaje = "El valor de activo no cumple el formato esperado por la base de datos.";
            }

            model.addAttribute("error", mensaje);
            model.addAttribute("errorTecnicoPago", detalle);
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el pago.");
            model.addAttribute("errorTecnicoPago", ex.getMessage());
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esEdicion ? "Pago actualizado correctamente." : "Pago registrado correctamente."
        );

        return "redirect:/admin/pagos";
    }

    @PostMapping("/admin/pagos/eliminar/{id}")
    public String eliminarPago(@PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes) {

        Optional<Pago> pagoOpt = pagoRepository.findById(id);

        if (pagoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El pago no existe.");
            return "redirect:/admin/pagos";
        }

        try {
            pagoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Pago eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el pago porque tiene relaciones activas en el sistema."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al intentar eliminar el pago."
            );
        }

        return "redirect:/admin/pagos";
    }

    private void prepararVistaPagos(Model model, Pago pagoForm, boolean modoEdicion) {
        inicializarRelacionesSiSonNull(pagoForm);

        model.addAttribute("pagosListado", pagoRepository.findAll().stream()
                .sorted(Comparator.comparing(Pago::getIdPago, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());

        List<Usuario> clientesConMembresia = usuarioRepository
                .findByRol_NombreIgnoreCaseOrderByNombreAscApellidoAsc("CLIENTE")
                .stream()
                .filter(u -> membresiaRepository.findByUsuarioIdUsuario(u.getIdUsuario()).isPresent())
                .toList();

        model.addAttribute("usuariosDisponibles", clientesConMembresia);

        model.addAttribute("metodosPagoDisponibles", metodoPagoRepository.findAll().stream()
                .sorted(Comparator.comparing(MetodoPago::getTipoTransferencia, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("cajasDisponibles", cajaRepository.findAll().stream()
                .sorted(Comparator.comparing(Caja::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("empleadosDisponibles", empleadoRepository.findAll().stream()
                .sorted(Comparator.comparing(Empleado::getIdEmpleado, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());

        String resumenMembresia = "";
        if (pagoForm.getUsuario() != null && pagoForm.getUsuario().getIdUsuario() != null) {
            Optional<Membresia> membresiaOpt = membresiaRepository.findByUsuarioIdUsuario(pagoForm.getUsuario().getIdUsuario());
            if (membresiaOpt.isPresent()) {
                Membresia m = membresiaOpt.get();
                resumenMembresia = "ID " + m.getIdMembresia()
                        + " - "
                        + (m.getTipoMembresia() != null ? m.getTipoMembresia().getNombre() : "Sin tipo");
            }
        } else if (modoEdicion && pagoForm.getMembresia() != null && pagoForm.getMembresia().getIdMembresia() != null) {
            resumenMembresia = "ID " + pagoForm.getMembresia().getIdMembresia()
                    + " - "
                    + (pagoForm.getMembresia().getTipoMembresia() != null ? pagoForm.getMembresia().getTipoMembresia().getNombre() : "Sin tipo");
        }

        model.addAttribute("resumenMembresia", resumenMembresia);
        model.addAttribute("pagoForm", pagoForm);
        model.addAttribute("modoEdicionPago", modoEdicion);
    }

    private Pago nuevoPagoForm() {
        Pago pago = new Pago();
        inicializarRelacionesSiSonNull(pago);
        pago.setActivo("S");
        return pago;
    }

    private void inicializarRelacionesSiSonNull(Pago pago) {
        if (pago.getUsuario() == null) {
            pago.setUsuario(new Usuario());
        }
        if (pago.getMembresia() == null) {
            pago.setMembresia(new Membresia());
        }
        if (pago.getMetodoPago() == null) {
            pago.setMetodoPago(new MetodoPago());
        }
        if (pago.getCaja() == null) {
            pago.setCaja(new Caja());
        }
        if (pago.getEmpleado() == null) {
            pago.setEmpleado(new Empleado());
        }
    }
}