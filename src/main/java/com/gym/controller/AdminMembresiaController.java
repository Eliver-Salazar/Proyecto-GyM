package com.gym.controller;

import com.gym.Repository.EstadoMembresiaRepository;
import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.SucursalRepository;
import com.gym.Repository.TipoMembresiaRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.EstadoMembresia;
import com.gym.domain.Membresia;
import com.gym.domain.Sucursal;
import com.gym.domain.TipoMembresia;
import com.gym.domain.Usuario;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminMembresiaController {

    private final MembresiaRepository membresiaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoMembresiaRepository tipoMembresiaRepository;
    private final EstadoMembresiaRepository estadoMembresiaRepository;
    private final SucursalRepository sucursalRepository;

    public AdminMembresiaController(MembresiaRepository membresiaRepository,
                                    UsuarioRepository usuarioRepository,
                                    TipoMembresiaRepository tipoMembresiaRepository,
                                    EstadoMembresiaRepository estadoMembresiaRepository,
                                    SucursalRepository sucursalRepository) {
        this.membresiaRepository = membresiaRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoMembresiaRepository = tipoMembresiaRepository;
        this.estadoMembresiaRepository = estadoMembresiaRepository;
        this.sucursalRepository = sucursalRepository;
    }

    @GetMapping("/admin/membresias")
    public String membresias(Model model) {
        prepararVistaMembresias(model, nuevaMembresiaForm(), false);
        return "admin/membresias";
    }

    @GetMapping("/admin/membresias/editar/{id}")
    public String editarMembresia(@PathVariable("id") Long id,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        Optional<Membresia> membresiaOpt = membresiaRepository.findById(id);

        if (membresiaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La membresía solicitada no existe.");
            return "redirect:/admin/membresias";
        }

        prepararVistaMembresias(model, membresiaOpt.get(), true);
        return "admin/membresias";
    }

    @PostMapping("/admin/membresias/guardar")
    public String guardarMembresia(@ModelAttribute("membresiaForm") Membresia membresiaForm,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        boolean esEdicion = membresiaForm.getIdMembresia() != null;
        inicializarRelacionesSiSonNull(membresiaForm);

        if (membresiaForm.getUsuario().getIdUsuario() == null
                || membresiaForm.getTipoMembresia().getIdTipoMembresia() == null
                || membresiaForm.getEstadoMembresia().getIdEstadoMembresia() == null
                || membresiaForm.getSucursal().getIdSucursal() == null
                || membresiaForm.getFechaInicio() == null
                || membresiaForm.getFechaVencimiento() == null) {

            model.addAttribute("error",
                    "Debes seleccionar cliente, tipo, estado, sucursal y completar ambas fechas.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(membresiaForm.getUsuario().getIdUsuario());
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("error", "El cliente seleccionado no existe.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        Usuario usuarioSeleccionado = usuarioOpt.get();
        if (usuarioSeleccionado.getRol() == null
                || usuarioSeleccionado.getRol().getNombre() == null
                || !"CLIENTE".equalsIgnoreCase(usuarioSeleccionado.getRol().getNombre())) {
            model.addAttribute("error", "Solo se pueden asignar membresías a usuarios con rol CLIENTE.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        Optional<Membresia> membresiaExistenteUsuario =
                membresiaRepository.findByUsuarioIdUsuario(usuarioSeleccionado.getIdUsuario());

        if (membresiaExistenteUsuario.isPresent()
                && (membresiaForm.getIdMembresia() == null
                || !membresiaExistenteUsuario.get().getIdMembresia().equals(membresiaForm.getIdMembresia()))) {
            model.addAttribute("error",
                    "El cliente seleccionado ya tiene una membresía registrada. Debes editar la existente.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        Optional<TipoMembresia> tipoOpt =
                tipoMembresiaRepository.findById(membresiaForm.getTipoMembresia().getIdTipoMembresia());
        if (tipoOpt.isEmpty()) {
            model.addAttribute("error", "El tipo de membresía seleccionado no existe.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        Optional<EstadoMembresia> estadoOpt =
                estadoMembresiaRepository.findById(membresiaForm.getEstadoMembresia().getIdEstadoMembresia());
        if (estadoOpt.isEmpty()) {
            model.addAttribute("error", "El estado de membresía seleccionado no existe.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        Optional<Sucursal> sucursalOpt =
                sucursalRepository.findById(membresiaForm.getSucursal().getIdSucursal());
        if (sucursalOpt.isEmpty()) {
            model.addAttribute("error", "La sucursal seleccionada no existe.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        if (membresiaForm.getFechaInicio().after(membresiaForm.getFechaVencimiento())) {
            model.addAttribute("error", "La fecha de inicio no puede ser mayor que la fecha de vencimiento.");
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        Membresia membresiaGuardar;

        if (esEdicion) {
            Optional<Membresia> membresiaOpt = membresiaRepository.findById(membresiaForm.getIdMembresia());
            if (membresiaOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró la membresía a editar.");
                return "redirect:/admin/membresias";
            }
            membresiaGuardar = membresiaOpt.get();
        } else {
            membresiaGuardar = new Membresia();
        }

        membresiaGuardar.setUsuario(usuarioSeleccionado);
        membresiaGuardar.setTipoMembresia(tipoOpt.get());
        membresiaGuardar.setEstadoMembresia(estadoOpt.get());
        membresiaGuardar.setSucursal(sucursalOpt.get());
        membresiaGuardar.setFechaInicio(membresiaForm.getFechaInicio());
        membresiaGuardar.setFechaVencimiento(membresiaForm.getFechaVencimiento());

        try {
            membresiaRepository.save(membresiaGuardar);
        }   catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            String detalleUpper = detalle != null ? detalle.toUpperCase() : "";
            String mensaje = "No se pudo guardar la membresía por una restricción de base de datos.";

            if (detalleUpper.contains("MEMBRESIAS_FECHAS_CHK") || detalleUpper.contains("ORA-02290")) {
                mensaje = "La fecha de inicio no puede ser mayor que la fecha de vencimiento.";
            } else if (detalleUpper.contains("MEMBRESIAS_USUARIOS_FK") || detalleUpper.contains("ORA-02291")) {
                mensaje = "El cliente seleccionado no existe o no es válido en la base de datos.";
            } else if (detalleUpper.contains("MEMBRESIAS_TIPOS_FK")) {
                mensaje = "El tipo de membresía seleccionado no existe en la base de datos.";
            } else if (detalleUpper.contains("MEMBRESIAS_ESTADOS_FK")) {
                mensaje = "El estado de membresía seleccionado no existe en la base de datos.";
            } else if (detalleUpper.contains("MEMBRESIAS_SUCURSALES_FK")) {
                mensaje = "La sucursal seleccionada no existe en la base de datos.";
            } else if (detalleUpper.contains("UNIQUE") || detalleUpper.contains("ORA-00001")) {
                mensaje = "El cliente seleccionado ya tiene una membresía registrada. Debes editar la existente.";
            }

            model.addAttribute("error", mensaje);
            model.addAttribute("errorTecnicoMembresia", detalle);
            prepararVistaMembresias(model, membresiaForm, esEdicion);
            return "admin/membresias";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esEdicion ? "Membresía actualizada correctamente." : "Membresía creada correctamente."
        );

        return "redirect:/admin/membresias";
    }

    @PostMapping("/admin/membresias/eliminar/{id}")
    public String eliminarMembresia(@PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {

        Optional<Membresia> membresiaOpt = membresiaRepository.findById(id);

        if (membresiaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La membresía no existe.");
            return "redirect:/admin/membresias";
        }

        try {
            membresiaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Membresía eliminada correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar la membresía porque tiene relaciones activas en el sistema."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al intentar eliminar la membresía."
            );
        }

        return "redirect:/admin/membresias";
    }

    private void prepararVistaMembresias(Model model, Membresia membresiaForm, boolean modoEdicion) {
        inicializarRelacionesSiSonNull(membresiaForm);

        model.addAttribute("membresiasListado", membresiaRepository.findAll().stream()
                .sorted(Comparator.comparing(Membresia::getIdMembresia, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());

        List<Usuario> clientes = usuarioRepository.findByRol_NombreIgnoreCaseOrderByNombreAscApellidoAsc("CLIENTE");

        List<Usuario> clientesDisponibles = clientes.stream()
                .filter(u -> {
                    Optional<Membresia> membresiaOpt = membresiaRepository.findByUsuarioIdUsuario(u.getIdUsuario());
                    return membresiaOpt.isEmpty()
                            || (membresiaForm.getIdMembresia() != null
                            && membresiaOpt.get().getIdMembresia().equals(membresiaForm.getIdMembresia()));
                })
                .toList();

        model.addAttribute("usuariosDisponibles", clientesDisponibles);

        model.addAttribute("tiposDisponibles", tipoMembresiaRepository.findAll().stream()
                .sorted(Comparator.comparing(TipoMembresia::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("estadosDisponibles", estadoMembresiaRepository.findAll().stream()
                .sorted(Comparator.comparing(EstadoMembresia::getNombreEstado, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("sucursalesDisponibles", sucursalRepository.findAll().stream()
                .sorted(Comparator.comparing(Sucursal::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("membresiaForm", membresiaForm);
        model.addAttribute("modoEdicionMembresia", modoEdicion);
    }

    private Membresia nuevaMembresiaForm() {
        Membresia membresia = new Membresia();
        inicializarRelacionesSiSonNull(membresia);
        return membresia;
    }

    private void inicializarRelacionesSiSonNull(Membresia membresia) {
        if (membresia.getUsuario() == null) {
            membresia.setUsuario(new Usuario());
        }
        if (membresia.getTipoMembresia() == null) {
            membresia.setTipoMembresia(new TipoMembresia());
        }
        if (membresia.getEstadoMembresia() == null) {
            membresia.setEstadoMembresia(new EstadoMembresia());
        }
        if (membresia.getSucursal() == null) {
            membresia.setSucursal(new Sucursal());
        }
    }
}