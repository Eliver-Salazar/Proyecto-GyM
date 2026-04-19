package com.gym.controller;

import com.gym.Repository.CajaRepository;
import com.gym.Repository.EmpleadoRepository;
import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.MetodoPagoRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.TipoMembresiaRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Caja;
import com.gym.domain.Empleado;
import com.gym.domain.Membresia;
import com.gym.domain.MetodoPago;
import com.gym.domain.Pago;
import com.gym.domain.TipoMembresia;
import com.gym.domain.Usuario;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
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
    private final TipoMembresiaRepository tipoMembresiaRepository;

    public AdminPagoController(PagoRepository pagoRepository,
                               UsuarioRepository usuarioRepository,
                               MembresiaRepository membresiaRepository,
                               MetodoPagoRepository metodoPagoRepository,
                               CajaRepository cajaRepository,
                               EmpleadoRepository empleadoRepository,
                               TipoMembresiaRepository tipoMembresiaRepository) {
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.cajaRepository = cajaRepository;
        this.empleadoRepository = empleadoRepository;
        this.tipoMembresiaRepository = tipoMembresiaRepository;
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

        Pago pago = pagoOpt.get();
        inicializarRelacionesSiSonNull(pago);

        if (pago.getMembresia() != null && pago.getMembresia().getTipoMembresia() == null) {
            pago.getMembresia().setTipoMembresia(new TipoMembresia());
        }

        prepararVistaPagos(model, pago, true);
        return "admin/pagos";
    }

    @PostMapping("/admin/pagos/guardar")
    public String guardarPago(@ModelAttribute("pagoForm") Pago pagoForm,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        boolean esEdicion = pagoForm.getIdPago() != null;
        inicializarRelacionesSiSonNull(pagoForm);

        if (pagoForm.getMembresia().getTipoMembresia() == null) {
            pagoForm.getMembresia().setTipoMembresia(new TipoMembresia());
        }

        if (pagoForm.getUsuario().getIdUsuario() == null
                || pagoForm.getMembresia().getTipoMembresia().getIdTipoMembresia() == null
                || pagoForm.getMetodoPago().getIdMetodoPago() == null
                || pagoForm.getCaja().getIdCaja() == null
                || pagoForm.getEmpleado().getIdEmpleado() == null
                || pagoForm.getFechaPago() == null
                || pagoForm.getActivo() == null
                || pagoForm.getActivo().isBlank()) {

            model.addAttribute("error",
                    "Debes seleccionar cliente, tipo de membresía, método de pago, caja, empleado, fecha y estado activo.");
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

        Membresia membresiaSeleccionada = membresiaOpt.get();

        Optional<TipoMembresia> tipoOpt = tipoMembresiaRepository.findById(
                pagoForm.getMembresia().getTipoMembresia().getIdTipoMembresia()
        );
        if (tipoOpt.isEmpty()) {
            model.addAttribute("error", "El tipo de membresía seleccionado no existe.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        TipoMembresia tipoSeleccionado = tipoOpt.get();

        if (tipoSeleccionado.getPrecio() == null || tipoSeleccionado.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "El tipo de membresía seleccionado no tiene un precio válido.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        if (tipoSeleccionado.getCantidadDias() == null || tipoSeleccionado.getCantidadDias() <= 0) {
            model.addAttribute("error", "El tipo de membresía seleccionado no tiene una cantidad de días válida.");
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

        String activoNormalizado = pagoForm.getActivo().trim().toUpperCase();
        if (!"S".equals(activoNormalizado) && !"N".equals(activoNormalizado)) {
            model.addAttribute("error", "El campo activo debe ser 'S' o 'N'.");
            prepararVistaPagos(model, pagoForm, esEdicion);
            return "admin/pagos";
        }

        String referenciaNormalizada = null;
        if (pagoForm.getReferencia() != null && !pagoForm.getReferencia().isBlank()) {
            referenciaNormalizada = pagoForm.getReferencia().trim().replaceAll("\\s+", " ").toUpperCase();

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

        BigDecimal montoCalculado = tipoSeleccionado.getPrecio();

        // Actualiza la membresía única del cliente según el plan pagado
        membresiaSeleccionada.setTipoMembresia(tipoSeleccionado);
        membresiaSeleccionada.setFechaInicio(pagoForm.getFechaPago());
        membresiaSeleccionada.setFechaVencimiento(calcularFechaVencimiento(
                pagoForm.getFechaPago(),
                tipoSeleccionado.getCantidadDias()
        ));

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
        pagoGuardar.setMembresia(membresiaSeleccionada);
        pagoGuardar.setMetodoPago(metodoOpt.get());
        pagoGuardar.setCaja(cajaOpt.get());
        pagoGuardar.setEmpleado(empleadoOpt.get());
        pagoGuardar.setMonto(montoCalculado);
        pagoGuardar.setFechaPago(pagoForm.getFechaPago());
        pagoGuardar.setReferencia(referenciaNormalizada);
        pagoGuardar.setActivo(activoNormalizado);

        try {
            membresiaRepository.save(membresiaSeleccionada);
            pagoRepository.save(pagoGuardar);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            model.addAttribute("error", "No se pudo guardar el pago por una restricción de base de datos.");
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

        if (pagoForm.getMembresia().getTipoMembresia() == null) {
            pagoForm.getMembresia().setTipoMembresia(new TipoMembresia());
        }

        model.addAttribute("pagosListado", pagoRepository.findAll().stream()
                .sorted(Comparator.comparing(Pago::getIdPago, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());

        List<Usuario> clientesConMembresia = usuarioRepository
                .findByRol_NombreIgnoreCaseOrderByNombreAscApellidoAsc("CLIENTE")
                .stream()
                .filter(u -> membresiaRepository.findByUsuarioIdUsuario(u.getIdUsuario()).isPresent())
                .toList();

        model.addAttribute("usuariosDisponibles", clientesConMembresia);

        model.addAttribute("tiposMembresiaDisponibles", tipoMembresiaRepository.findAll().stream()
                .sorted(Comparator.comparing(TipoMembresia::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("metodosPagoDisponibles", metodoPagoRepository.findAll().stream()
                .sorted(Comparator.comparing(MetodoPago::getTipoTransferencia, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("cajasDisponibles", cajaRepository.findAll().stream()
                .sorted(Comparator.comparing(Caja::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("empleadosDisponibles", empleadoRepository.findAll().stream()
                .sorted(Comparator.comparing(Empleado::getIdEmpleado, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());

        String membresiaActualTexto = "Seleccione un cliente para consultar su membresía actual.";
        BigDecimal montoSugerido = null;

        if (pagoForm.getUsuario() != null && pagoForm.getUsuario().getIdUsuario() != null) {
            Optional<Membresia> membresiaOpt = membresiaRepository.findByUsuarioIdUsuario(pagoForm.getUsuario().getIdUsuario());
            if (membresiaOpt.isPresent()) {
                Membresia membresiaActual = membresiaOpt.get();
                String nombreTipoActual = membresiaActual.getTipoMembresia() != null
                        ? membresiaActual.getTipoMembresia().getNombre()
                        : "Sin tipo";
                membresiaActualTexto = "ID " + membresiaActual.getIdMembresia() + " - " + nombreTipoActual;

                if (modoEdicion && pagoForm.getMembresia().getTipoMembresia().getIdTipoMembresia() == null
                        && membresiaActual.getTipoMembresia() != null) {
                    pagoForm.getMembresia().getTipoMembresia()
                            .setIdTipoMembresia(membresiaActual.getTipoMembresia().getIdTipoMembresia());
                }
            }
        }

        if (pagoForm.getMembresia() != null
                && pagoForm.getMembresia().getTipoMembresia() != null
                && pagoForm.getMembresia().getTipoMembresia().getIdTipoMembresia() != null) {
            tipoMembresiaRepository.findById(pagoForm.getMembresia().getTipoMembresia().getIdTipoMembresia())
                    .ifPresent(tipo -> model.addAttribute("montoSugerido", tipo.getPrecio()));
        } else {
            model.addAttribute("montoSugerido", montoSugerido);
        }

        model.addAttribute("membresiaActualTexto", membresiaActualTexto);
        model.addAttribute("pagoForm", pagoForm);
        model.addAttribute("modoEdicionPago", modoEdicion);
    }

    private Pago nuevoPagoForm() {
        Pago pago = new Pago();
        inicializarRelacionesSiSonNull(pago);
        pago.getMembresia().setTipoMembresia(new TipoMembresia());
        pago.setActivo("S");
        pago.setFechaPago(new Date());
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

    private Date calcularFechaVencimiento(Date fechaInicio, Integer cantidadDias) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fechaInicio);
        calendar.add(Calendar.DAY_OF_MONTH, cantidadDias);
        return calendar.getTime();
    }
}