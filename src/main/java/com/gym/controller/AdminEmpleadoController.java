package com.gym.controller;

import com.gym.Repository.EmpleadoRepository;
import com.gym.Repository.SucursalRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Empleado;
import com.gym.domain.Sucursal;
import com.gym.domain.Usuario;
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
public class AdminEmpleadoController {

    private static final Pattern PUESTO_PATTERN =
            Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$");

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;

    public AdminEmpleadoController(EmpleadoRepository empleadoRepository,
                                   UsuarioRepository usuarioRepository,
                                   SucursalRepository sucursalRepository) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.sucursalRepository = sucursalRepository;
    }

    @GetMapping("/admin/empleados")
    public String empleados(Model model) {
        prepararVistaEmpleados(model, new Empleado(), false);
        return "admin/empleados";
    }

    @GetMapping("/admin/empleados/editar/{id}")
    public String editarEmpleado(@PathVariable("id") Long id,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        Optional<Empleado> empleadoOpt = empleadoRepository.findById(id);

        if (empleadoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El empleado solicitado no existe.");
            return "redirect:/admin/empleados";
        }

        prepararVistaEmpleados(model, empleadoOpt.get(), true);
        return "admin/empleados";
    }

    @PostMapping("/admin/empleados/guardar")
    public String guardarEmpleado(@ModelAttribute("empleadoForm") Empleado empleadoForm,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        boolean esEdicion = empleadoForm.getIdEmpleado() != null;

        if (empleadoForm.getUsuario() == null || empleadoForm.getUsuario().getIdUsuario() == null
                || empleadoForm.getSucursal() == null || empleadoForm.getSucursal().getIdSucursal() == null
                || empleadoForm.getPuesto() == null || empleadoForm.getPuesto().isBlank()
                || empleadoForm.getFechaContratacion() == null) {

            model.addAttribute("error",
                    "Debes seleccionar usuario, sucursal, puesto y fecha de contratación.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(empleadoForm.getUsuario().getIdUsuario());
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("error", "El usuario seleccionado no existe.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        Usuario usuarioSeleccionado = usuarioOpt.get();

        if (usuarioSeleccionado.getRol() == null
                || usuarioSeleccionado.getRol().getNombre() == null
                || !"PERSONAL".equalsIgnoreCase(usuarioSeleccionado.getRol().getNombre())) {
            model.addAttribute("error", "Solo se pueden registrar empleados a partir de usuarios con rol PERSONAL.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        Optional<Sucursal> sucursalOpt = sucursalRepository.findById(empleadoForm.getSucursal().getIdSucursal());
        if (sucursalOpt.isEmpty()) {
            model.addAttribute("error", "La sucursal seleccionada no existe.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        String puestoNormalizado = empleadoForm.getPuesto().trim().replaceAll("\\s+", " ").toUpperCase();
        empleadoForm.setPuesto(puestoNormalizado);

        if (puestoNormalizado.length() > 60) {
            model.addAttribute("error", "El puesto no puede superar los 60 caracteres.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        if (!PUESTO_PATTERN.matcher(puestoNormalizado).matches()) {
            model.addAttribute("error", "El puesto solo puede contener letras y espacios.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        Optional<Empleado> empleadoExistenteUsuario =
                empleadoRepository.findByUsuarioIdUsuario(usuarioSeleccionado.getIdUsuario());

        if (empleadoExistenteUsuario.isPresent()
                && (empleadoForm.getIdEmpleado() == null
                || !empleadoExistenteUsuario.get().getIdEmpleado().equals(empleadoForm.getIdEmpleado()))) {
            model.addAttribute("error", "El usuario seleccionado ya está asignado a otro empleado.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        Empleado empleadoGuardar;

        if (esEdicion) {
            Optional<Empleado> empleadoOpt = empleadoRepository.findById(empleadoForm.getIdEmpleado());
            if (empleadoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el empleado a editar.");
                return "redirect:/admin/empleados";
            }
            empleadoGuardar = empleadoOpt.get();
        } else {
            empleadoGuardar = new Empleado();
        }

        empleadoGuardar.setUsuario(usuarioSeleccionado);
        empleadoGuardar.setSucursal(sucursalOpt.get());
        empleadoGuardar.setPuesto(puestoNormalizado);
        empleadoGuardar.setFechaContratacion(empleadoForm.getFechaContratacion());

        try {
            empleadoRepository.save(empleadoGuardar);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            String mensaje = "No se pudo guardar el empleado por una restricción de base de datos.";

            if (detalle != null && detalle.toUpperCase().contains("UNIQUE")) {
                mensaje = "El usuario seleccionado ya está asignado a otro empleado.";
            }

            model.addAttribute("error", mensaje);
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el empleado.");
            prepararVistaEmpleados(model, empleadoForm, esEdicion);
            return "admin/empleados";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esEdicion ? "Empleado actualizado correctamente." : "Empleado creado correctamente."
        );

        return "redirect:/admin/empleados";
    }

    @PostMapping("/admin/empleados/eliminar/{id}")
    public String eliminarEmpleado(@PathVariable("id") Long id,
                                   RedirectAttributes redirectAttributes) {

        Optional<Empleado> empleadoOpt = empleadoRepository.findById(id);

        if (empleadoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El empleado no existe.");
            return "redirect:/admin/empleados";
        }

        try {
            empleadoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Empleado eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el empleado porque tiene relaciones activas en el sistema."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al intentar eliminar el empleado."
            );
        }

        return "redirect:/admin/empleados";
    }

    private void prepararVistaEmpleados(Model model, Empleado empleadoForm, boolean modoEdicion) {
        model.addAttribute("empleadosListado", empleadoRepository.findAll().stream()
                .sorted(Comparator.comparing(Empleado::getIdEmpleado, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());

        List<Usuario> usuariosPersonal = usuarioRepository
                .findByRol_NombreIgnoreCaseOrderByNombreAscApellidoAsc("PERSONAL");

        List<Usuario> usuariosDisponibles = usuariosPersonal.stream()
                .filter(u -> {
                    Optional<Empleado> empleadoOpt = empleadoRepository.findByUsuarioIdUsuario(u.getIdUsuario());
                    return empleadoOpt.isEmpty()
                            || (empleadoForm.getIdEmpleado() != null
                            && empleadoOpt.get().getIdEmpleado().equals(empleadoForm.getIdEmpleado()));
                })
                .toList();

        model.addAttribute("usuariosDisponibles", usuariosDisponibles);
        model.addAttribute("sucursalesDisponibles", sucursalRepository.findAll().stream()
                .sorted(Comparator.comparing(Sucursal::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("empleadoForm", empleadoForm);
        model.addAttribute("modoEdicionEmpleado", modoEdicion);
    }
}