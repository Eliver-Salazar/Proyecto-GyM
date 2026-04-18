package com.gym.controller;

import com.gym.Repository.CajaRepository;
import com.gym.Repository.SucursalRepository;
import com.gym.domain.Caja;
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
public class AdminCajaController {

    private static final Pattern NOMBRE_CAJA_PATTERN =
            Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9 ]+$");

    private final CajaRepository cajaRepository;
    private final SucursalRepository sucursalRepository;

    public AdminCajaController(CajaRepository cajaRepository,
                               SucursalRepository sucursalRepository) {
        this.cajaRepository = cajaRepository;
        this.sucursalRepository = sucursalRepository;
    }

    @GetMapping("/admin/cajas")
    public String cajas(Model model) {
        prepararVistaCajas(model, new Caja(), false);
        return "admin/cajas";
    }

    @GetMapping("/admin/cajas/editar/{id}")
    public String editarCaja(@PathVariable("id") Long id,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        Optional<Caja> cajaOpt = cajaRepository.findById(id);

        if (cajaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La caja solicitada no existe.");
            return "redirect:/admin/cajas";
        }

        prepararVistaCajas(model, cajaOpt.get(), true);
        return "admin/cajas";
    }

    @PostMapping("/admin/cajas/guardar")
    public String guardarCaja(@ModelAttribute("cajaForm") Caja cajaForm,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        boolean esEdicion = cajaForm.getIdCaja() != null;

        if (cajaForm.getSucursal() == null || cajaForm.getSucursal().getIdSucursal() == null
                || cajaForm.getNombre() == null || cajaForm.getNombre().isBlank()) {
            model.addAttribute("error", "Debes seleccionar la sucursal y escribir el nombre de la caja.");
            prepararVistaCajas(model, cajaForm, esEdicion);
            return "admin/cajas";
        }

        Optional<Sucursal> sucursalOpt = sucursalRepository.findById(cajaForm.getSucursal().getIdSucursal());
        if (sucursalOpt.isEmpty()) {
            model.addAttribute("error", "La sucursal seleccionada no existe.");
            prepararVistaCajas(model, cajaForm, esEdicion);
            return "admin/cajas";
        }

        String nombreNormalizado = cajaForm.getNombre().trim().replaceAll("\\s+", " ").toUpperCase();
        cajaForm.setNombre(nombreNormalizado);

        if (nombreNormalizado.length() > 40) {
            model.addAttribute("error", "El nombre de la caja no puede superar los 40 caracteres.");
            prepararVistaCajas(model, cajaForm, esEdicion);
            return "admin/cajas";
        }

        if (!NOMBRE_CAJA_PATTERN.matcher(nombreNormalizado).matches()) {
            model.addAttribute("error", "El nombre de la caja solo puede contener letras, números y espacios.");
            prepararVistaCajas(model, cajaForm, esEdicion);
            return "admin/cajas";
        }

        Optional<Caja> cajaDuplicada = cajaRepository.findBySucursalIdSucursalAndNombreIgnoreCase(
                sucursalOpt.get().getIdSucursal(),
                nombreNormalizado
        );

        if (cajaDuplicada.isPresent()
                && (cajaForm.getIdCaja() == null
                || !cajaDuplicada.get().getIdCaja().equals(cajaForm.getIdCaja()))) {
            model.addAttribute("error", "Ya existe una caja con ese nombre en la sucursal seleccionada.");
            prepararVistaCajas(model, cajaForm, esEdicion);
            return "admin/cajas";
        }

        Caja cajaGuardar;

        if (esEdicion) {
            Optional<Caja> cajaOpt = cajaRepository.findById(cajaForm.getIdCaja());
            if (cajaOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró la caja a editar.");
                return "redirect:/admin/cajas";
            }
            cajaGuardar = cajaOpt.get();
        } else {
            cajaGuardar = new Caja();
        }

        cajaGuardar.setSucursal(sucursalOpt.get());
        cajaGuardar.setNombre(nombreNormalizado);

        try {
            cajaRepository.save(cajaGuardar);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            String mensaje = "No se pudo guardar la caja por una restricción de base de datos.";

            if (detalle != null && detalle.toUpperCase().contains("UNIQUE")) {
                mensaje = "Ya existe una caja con ese nombre en la sucursal seleccionada.";
            }

            model.addAttribute("error", mensaje);
            prepararVistaCajas(model, cajaForm, esEdicion);
            return "admin/cajas";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar la caja.");
            prepararVistaCajas(model, cajaForm, esEdicion);
            return "admin/cajas";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esEdicion ? "Caja actualizada correctamente." : "Caja creada correctamente."
        );

        return "redirect:/admin/cajas";
    }

    @PostMapping("/admin/cajas/eliminar/{id}")
    public String eliminarCaja(@PathVariable("id") Long id,
                               RedirectAttributes redirectAttributes) {

        Optional<Caja> cajaOpt = cajaRepository.findById(id);

        if (cajaOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La caja no existe.");
            return "redirect:/admin/cajas";
        }

        try {
            cajaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Caja eliminada correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar la caja porque tiene relaciones activas en el sistema."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al intentar eliminar la caja."
            );
        }

        return "redirect:/admin/cajas";
    }

    private void prepararVistaCajas(Model model, Caja cajaForm, boolean modoEdicion) {
        model.addAttribute("cajasListado", cajaRepository.findAll().stream()
                .sorted(Comparator.comparing(Caja::getIdCaja, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());

        model.addAttribute("sucursalesDisponibles", sucursalRepository.findAll().stream()
                .sorted(Comparator.comparing(Sucursal::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());

        model.addAttribute("cajaForm", cajaForm);
        model.addAttribute("modoEdicionCaja", modoEdicion);
    }
}