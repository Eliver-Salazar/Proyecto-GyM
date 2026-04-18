package com.gym.controller;

import com.gym.Repository.RolRepository;
import com.gym.domain.Rol;
import java.util.Comparator;
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
public class AdminRolController {

    private final RolRepository rolRepository;

    public AdminRolController(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @GetMapping("/admin/roles")
    public String roles(Model model) {
        prepararVistaRoles(model, new Rol(), false);
        return "admin/roles";
    }

    @GetMapping("/admin/roles/editar/{id}")
    public String editarRol(@PathVariable("id") Long id,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        Optional<Rol> rolOpt = rolRepository.findById(id);

        if (rolOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El rol solicitado no existe.");
            return "redirect:/admin/roles";
        }

        prepararVistaRoles(model, rolOpt.get(), true);
        return "admin/roles";
    }

    @PostMapping("/admin/roles/guardar")
    public String guardarRol(@ModelAttribute("rolForm") Rol rolForm,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (rolForm.getNombre() == null || rolForm.getNombre().isBlank()) {
            model.addAttribute("error", "El nombre del rol es obligatorio.");
            prepararVistaRoles(model, rolForm, rolForm.getIdRol() != null);
            return "admin/roles";
        }

        String nombreNormalizado = rolForm.getNombre().trim().toUpperCase();

        Optional<Rol> rolExistenteNombre = rolRepository.findByNombre(nombreNormalizado);

        if (rolExistenteNombre.isPresent()
                && (rolForm.getIdRol() == null || !rolExistenteNombre.get().getIdRol().equals(rolForm.getIdRol()))) {
            model.addAttribute("error", "Ya existe un rol registrado con ese nombre.");
            rolForm.setNombre(nombreNormalizado);
            prepararVistaRoles(model, rolForm, rolForm.getIdRol() != null);
            return "admin/roles";
        }

        boolean esEdicion = rolForm.getIdRol() != null;
        Rol rolGuardar;

        if (esEdicion) {
            Optional<Rol> rolOpt = rolRepository.findById(rolForm.getIdRol());
            if (rolOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el rol a editar.");
                return "redirect:/admin/roles";
            }
            rolGuardar = rolOpt.get();
        } else {
            rolGuardar = new Rol();
        }

        rolGuardar.setNombre(nombreNormalizado);

        try {
            rolRepository.save(rolGuardar);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar el rol porque ya existe o viola una restricción.");
            prepararVistaRoles(model, rolForm, esEdicion);
            return "admin/roles";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el rol.");
            prepararVistaRoles(model, rolForm, esEdicion);
            return "admin/roles";
        }

        redirectAttributes.addFlashAttribute("mensaje",
                esEdicion ? "Rol actualizado correctamente." : "Rol creado correctamente.");

        return "redirect:/admin/roles";
    }

    @PostMapping("/admin/roles/eliminar/{id}")
    public String eliminarRol(@PathVariable("id") Long id,
                              RedirectAttributes redirectAttributes) {

        Optional<Rol> rolOpt = rolRepository.findById(id);

        if (rolOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El rol no existe.");
            return "redirect:/admin/roles";
        }

        try {
            rolRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Rol eliminado correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo eliminar el rol porque está siendo utilizado por uno o más usuarios.");
        }

        return "redirect:/admin/roles";
    }

    private void prepararVistaRoles(Model model, Rol rolForm, boolean modoEdicion) {
        model.addAttribute("rolesListado", rolRepository.findAll().stream()
                .sorted(Comparator.comparing(Rol::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("rolForm", rolForm);
        model.addAttribute("modoEdicionRol", modoEdicion);
    }
}