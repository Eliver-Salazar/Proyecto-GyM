package com.gym.controller;

import com.gym.Repository.SucursalRepository;
import com.gym.domain.Sucursal;
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
public class AdminSucursalController {

    private static final Pattern NOMBRE_SUCURSAL_PATTERN =
            Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9 ]+$");

    private final SucursalRepository sucursalRepository;

    public AdminSucursalController(SucursalRepository sucursalRepository) {
        this.sucursalRepository = sucursalRepository;
    }

    @GetMapping("/admin/sucursales")
    public String sucursales(Model model) {
        prepararVistaSucursales(model, new Sucursal(), false);
        return "admin/sucursales";
    }

    @GetMapping("/admin/sucursales/editar/{id}")
    public String editarSucursal(@PathVariable("id") Long id,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        Optional<Sucursal> sucursalOpt = sucursalRepository.findById(id);

        if (sucursalOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La sucursal solicitada no existe.");
            return "redirect:/admin/sucursales";
        }

        prepararVistaSucursales(model, sucursalOpt.get(), true);
        return "admin/sucursales";
    }

    @PostMapping("/admin/sucursales/guardar")
    public String guardarSucursal(@ModelAttribute("sucursalForm") Sucursal sucursalForm,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        boolean esEdicion = sucursalForm.getIdSucursal() != null;

        if (sucursalForm.getNombre() == null || sucursalForm.getNombre().isBlank()
                || sucursalForm.getDireccion() == null || sucursalForm.getDireccion().isBlank()) {
            model.addAttribute("error", "Debes completar el nombre y la dirección de la sucursal.");
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        }

        String nombreNormalizado = sucursalForm.getNombre().trim().replaceAll("\\s+", " ").toUpperCase();
        String direccionNormalizada = sucursalForm.getDireccion().trim().replaceAll("\\s+", " ");
        String telefonoNormalizado = limpiarTelefono(sucursalForm.getTelefono());

        sucursalForm.setNombre(nombreNormalizado);
        sucursalForm.setDireccion(direccionNormalizada);
        sucursalForm.setTelefono(telefonoNormalizado);

        if (nombreNormalizado.length() > 80) {
            model.addAttribute("error", "El nombre de la sucursal no puede superar los 80 caracteres.");
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        }

        if (!NOMBRE_SUCURSAL_PATTERN.matcher(nombreNormalizado).matches()) {
            model.addAttribute("error", "El nombre de la sucursal solo puede contener letras, números y espacios.");
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        }

        if (direccionNormalizada.length() > 200) {
            model.addAttribute("error", "La dirección no puede superar los 200 caracteres.");
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        }

        if (telefonoNormalizado != null && !telefonoNormalizado.isBlank() && !telefonoNormalizado.matches("\\d{8}")) {
            model.addAttribute("error", "El teléfono de la sucursal debe contener exactamente 8 dígitos.");
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        }

        Optional<Sucursal> sucursalDuplicada = sucursalRepository.findByNombreIgnoreCase(nombreNormalizado);
        if (sucursalDuplicada.isPresent()
                && (sucursalForm.getIdSucursal() == null
                || !sucursalDuplicada.get().getIdSucursal().equals(sucursalForm.getIdSucursal()))) {
            model.addAttribute("error", "Ya existe una sucursal registrada con ese nombre.");
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        }

        Sucursal sucursalGuardar;

        if (esEdicion) {
            Optional<Sucursal> sucursalOpt = sucursalRepository.findById(sucursalForm.getIdSucursal());
            if (sucursalOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró la sucursal a editar.");
                return "redirect:/admin/sucursales";
            }
            sucursalGuardar = sucursalOpt.get();
        } else {
            sucursalGuardar = new Sucursal();
        }

        sucursalGuardar.setNombre(nombreNormalizado);
        sucursalGuardar.setDireccion(direccionNormalizada);
        sucursalGuardar.setTelefono((telefonoNormalizado == null || telefonoNormalizado.isBlank()) ? null : telefonoNormalizado);

        try {
            sucursalRepository.save(sucursalGuardar);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            String mensaje = "No se pudo guardar la sucursal por una restricción de base de datos.";

            if (detalle != null && detalle.toUpperCase().contains("UNIQUE")) {
                mensaje = "Ya existe una sucursal registrada con ese nombre.";
            }

            model.addAttribute("error", mensaje);
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar la sucursal.");
            prepararVistaSucursales(model, sucursalForm, esEdicion);
            return "admin/sucursales";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esEdicion ? "Sucursal actualizada correctamente." : "Sucursal creada correctamente."
        );

        return "redirect:/admin/sucursales";
    }

    @PostMapping("/admin/sucursales/eliminar/{id}")
    public String eliminarSucursal(@PathVariable("id") Long id,
                                   RedirectAttributes redirectAttributes) {

        Optional<Sucursal> sucursalOpt = sucursalRepository.findById(id);

        if (sucursalOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La sucursal no existe.");
            return "redirect:/admin/sucursales";
        }

        try {
            sucursalRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Sucursal eliminada correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar la sucursal porque tiene relaciones activas en el sistema."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al intentar eliminar la sucursal."
            );
        }

        return "redirect:/admin/sucursales";
    }

    private void prepararVistaSucursales(Model model, Sucursal sucursalForm, boolean modoEdicion) {
        model.addAttribute("sucursalesListado", sucursalRepository.findAll().stream()
                .sorted(Comparator.comparing(Sucursal::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("sucursalForm", sucursalForm);
        model.addAttribute("modoEdicionSucursal", modoEdicion);
    }

    private String limpiarTelefono(String telefono) {
        if (telefono == null) {
            return null;
        }
        String limpio = telefono.replaceAll("[^0-9]", "").trim();
        return limpio.isBlank() ? null : limpio;
    }
}