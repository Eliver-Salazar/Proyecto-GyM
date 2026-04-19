package com.gym.controller;

import com.gym.Repository.EstadoMembresiaRepository;
import com.gym.domain.EstadoMembresia;
import java.util.Comparator;
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
public class AdminEstadoMembresiaController {

    private static final Pattern SOLO_LETRAS_Y_ESPACIOS =
            Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$");

    private final EstadoMembresiaRepository estadoMembresiaRepository;

    public AdminEstadoMembresiaController(EstadoMembresiaRepository estadoMembresiaRepository) {
        this.estadoMembresiaRepository = estadoMembresiaRepository;
    }

    @GetMapping("/admin/estados-membresia")
    public String listarEstados(Model model) {
        prepararVista(model, new EstadoMembresia(), false);
        return "admin/estados-membresia";
    }

    @GetMapping("/admin/estados-membresia/editar/{id}")
    public String editarEstado(@PathVariable("id") Long id,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        Optional<EstadoMembresia> estadoOpt = estadoMembresiaRepository.findById(id);

        if (estadoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El estado de membresía solicitado no existe.");
            return "redirect:/admin/estados-membresia";
        }

        prepararVista(model, estadoOpt.get(), true);
        return "admin/estados-membresia";
    }

    @PostMapping("/admin/estados-membresia/guardar")
    public String guardarEstado(@ModelAttribute("estadoForm") EstadoMembresia estadoForm,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        boolean esEdicion = estadoForm.getIdEstadoMembresia() != null;

        if (estadoForm.getNombreEstado() == null || estadoForm.getNombreEstado().isBlank()) {
            model.addAttribute("error", "El nombre del estado es obligatorio.");
            prepararVista(model, estadoForm, esEdicion);
            return "admin/estados-membresia";
        }

        String nombreNormalizado = estadoForm.getNombreEstado().trim().replaceAll("\\s+", " ").toUpperCase();

        estadoForm.setNombreEstado(nombreNormalizado);

        if (nombreNormalizado.length() > 30) {
            model.addAttribute("error", "El nombre del estado no puede superar los 30 caracteres.");
            prepararVista(model, estadoForm, esEdicion);
            return "admin/estados-membresia";
        }

        if (!SOLO_LETRAS_Y_ESPACIOS.matcher(nombreNormalizado).matches()) {
            model.addAttribute("error", "El nombre del estado solo puede contener letras y espacios.");
            prepararVista(model, estadoForm, esEdicion);
            return "admin/estados-membresia";
        }

        Optional<EstadoMembresia> duplicadoOpt =
                estadoMembresiaRepository.findByNombreEstadoIgnoreCase(nombreNormalizado);

        if (duplicadoOpt.isPresent()
                && (estadoForm.getIdEstadoMembresia() == null
                || !duplicadoOpt.get().getIdEstadoMembresia().equals(estadoForm.getIdEstadoMembresia()))) {
            model.addAttribute("error", "Ya existe un estado de membresía registrado con ese nombre.");
            prepararVista(model, estadoForm, esEdicion);
            return "admin/estados-membresia";
        }

        EstadoMembresia estadoGuardar;

        if (esEdicion) {
            Optional<EstadoMembresia> existenteOpt =
                    estadoMembresiaRepository.findById(estadoForm.getIdEstadoMembresia());

            if (existenteOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el estado de membresía a editar.");
                return "redirect:/admin/estados-membresia";
            }

            estadoGuardar = existenteOpt.get();
        } else {
            estadoGuardar = new EstadoMembresia();
        }

        estadoGuardar.setNombreEstado(nombreNormalizado);

        try {
            estadoMembresiaRepository.save(estadoGuardar);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            if (detalle != null && detalle.toUpperCase().contains("UNIQUE")) {
                model.addAttribute("error", "No se pudo guardar porque el nombre del estado ya existe.");
            } else {
                model.addAttribute("error", "No se pudo guardar el estado por una restricción de base de datos.");
            }

            prepararVista(model, estadoForm, esEdicion);
            return "admin/estados-membresia";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el estado de membresía.");
            prepararVista(model, estadoForm, esEdicion);
            return "admin/estados-membresia";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esEdicion
                        ? "Estado de membresía actualizado correctamente."
                        : "Estado de membresía creado correctamente."
        );

        return "redirect:/admin/estados-membresia";
    }

    @PostMapping("/admin/estados-membresia/eliminar/{id}")
    public String eliminarEstado(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes) {

        Optional<EstadoMembresia> estadoOpt = estadoMembresiaRepository.findById(id);

        if (estadoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El estado de membresía no existe.");
            return "redirect:/admin/estados-membresia";
        }

        try {
            estadoMembresiaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Estado de membresía eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el estado porque está siendo utilizado por una o más membresías."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al intentar eliminar el estado de membresía."
            );
        }

        return "redirect:/admin/estados-membresia";
    }

    private void prepararVista(Model model, EstadoMembresia estadoForm, boolean modoEdicion) {
        model.addAttribute(
                "estadosListado",
                estadoMembresiaRepository.findAll().stream()
                        .sorted(Comparator.comparing(
                                EstadoMembresia::getNombreEstado,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        ))
                        .toList()
        );
        model.addAttribute("estadoForm", estadoForm);
        model.addAttribute("modoEdicionEstado", modoEdicion);
    }
}