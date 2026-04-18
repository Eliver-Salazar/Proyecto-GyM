package com.gym.controller;

import com.gym.Repository.TipoMembresiaRepository;
import com.gym.domain.TipoMembresia;
import java.math.BigDecimal;
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
public class AdminTipoMembresiaController {

    private final TipoMembresiaRepository tipoMembresiaRepository;

    public AdminTipoMembresiaController(TipoMembresiaRepository tipoMembresiaRepository) {
        this.tipoMembresiaRepository = tipoMembresiaRepository;
    }

    @GetMapping("/admin/tipos-membresia")
    public String tiposMembresia(Model model) {
        prepararVistaTiposMembresia(model, new TipoMembresia(), false);
        return "admin/tipos-membresia";
    }

    @GetMapping("/admin/tipos-membresia/editar/{id}")
    public String editarTipoMembresia(@PathVariable("id") Long id,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {

        Optional<TipoMembresia> tipoOpt = tipoMembresiaRepository.findById(id);

        if (tipoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El tipo de membresía solicitado no existe.");
            return "redirect:/admin/tipos-membresia";
        }

        prepararVistaTiposMembresia(model, tipoOpt.get(), true);
        return "admin/tipos-membresia";
    }

    @PostMapping("/admin/tipos-membresia/guardar")
    public String guardarTipoMembresia(@ModelAttribute("tipoForm") TipoMembresia tipoForm,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        if (tipoForm.getNombre() == null || tipoForm.getNombre().isBlank()
                || tipoForm.getCantidadDias() == null
                || tipoForm.getPrecio() == null
                || tipoForm.getAccesoGlobal() == null || tipoForm.getAccesoGlobal().isBlank()) {

            model.addAttribute("error",
                    "Debes completar nombre, cantidad de días, precio y acceso global.");
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        String nombreNormalizado = tipoForm.getNombre().trim().toUpperCase();
        String accesoGlobalNormalizado = tipoForm.getAccesoGlobal().trim().toUpperCase();

        if (tipoForm.getCantidadDias() <= 0) {
            model.addAttribute("error", "La cantidad de días debe ser mayor que cero.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        if (tipoForm.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "El precio no puede ser negativo.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        if (!accesoGlobalNormalizado.equals("S") && !accesoGlobalNormalizado.equals("N")) {
            model.addAttribute("error", "El acceso global debe ser 'S' o 'N'.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        List<TipoMembresia> existentes = tipoMembresiaRepository.findAll();
        boolean nombreDuplicado = existentes.stream()
                .anyMatch(t -> t.getNombre() != null
                        && t.getNombre().equalsIgnoreCase(nombreNormalizado)
                        && (tipoForm.getIdTipoMembresia() == null
                        || !t.getIdTipoMembresia().equals(tipoForm.getIdTipoMembresia())));

        if (nombreDuplicado) {
            model.addAttribute("error", "Ya existe un tipo de membresía registrado con ese nombre.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        boolean esEdicion = tipoForm.getIdTipoMembresia() != null;
        TipoMembresia tipoGuardar;

        if (esEdicion) {
            Optional<TipoMembresia> tipoOpt = tipoMembresiaRepository.findById(tipoForm.getIdTipoMembresia());
            if (tipoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el tipo de membresía a editar.");
                return "redirect:/admin/tipos-membresia";
            }
            tipoGuardar = tipoOpt.get();
        } else {
            tipoGuardar = new TipoMembresia();
        }

        tipoGuardar.setNombre(nombreNormalizado);
        tipoGuardar.setCantidadDias(tipoForm.getCantidadDias());
        tipoGuardar.setPrecio(tipoForm.getPrecio());
        tipoGuardar.setAccesoGlobal(accesoGlobalNormalizado);

        try {
            tipoMembresiaRepository.save(tipoGuardar);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error",
                    "No se pudo guardar el tipo de membresía porque ya existe o viola una restricción.");
            prepararVistaTiposMembresia(model, tipoForm, esEdicion);
            return "admin/tipos-membresia";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el tipo de membresía.");
            prepararVistaTiposMembresia(model, tipoForm, esEdicion);
            return "admin/tipos-membresia";
        }

        redirectAttributes.addFlashAttribute("mensaje",
                esEdicion ? "Tipo de membresía actualizado correctamente."
                          : "Tipo de membresía creado correctamente.");

        return "redirect:/admin/tipos-membresia";
    }

    @PostMapping("/admin/tipos-membresia/eliminar/{id}")
    public String eliminarTipoMembresia(@PathVariable("id") Long id,
                                        RedirectAttributes redirectAttributes) {

        Optional<TipoMembresia> tipoOpt = tipoMembresiaRepository.findById(id);

        if (tipoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El tipo de membresía no existe.");
            return "redirect:/admin/tipos-membresia";
        }

        try {
            tipoMembresiaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Tipo de membresía eliminado correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo eliminar el tipo de membresía porque está siendo utilizado por una o más membresías.");
        }

        return "redirect:/admin/tipos-membresia";
    }

    private void prepararVistaTiposMembresia(Model model, TipoMembresia tipoForm, boolean modoEdicion) {
        model.addAttribute("tiposListado", tipoMembresiaRepository.findAll().stream()
                .sorted(Comparator.comparing(TipoMembresia::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("tipoForm", tipoForm);
        model.addAttribute("modoEdicionTipo", modoEdicion);
    }
}