package com.gym.controller;

import com.gym.Repository.MetodoPagoRepository;
import com.gym.domain.MetodoPago;
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
public class AdminMetodoPagoController {

    private static final Pattern METODO_PAGO_PATTERN =
            Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñ0-9 ]+$");

    private final MetodoPagoRepository metodoPagoRepository;

    public AdminMetodoPagoController(MetodoPagoRepository metodoPagoRepository) {
        this.metodoPagoRepository = metodoPagoRepository;
    }

    @GetMapping("/admin/metodos-pago")
    public String metodosPago(Model model) {
        prepararVistaMetodosPago(model, new MetodoPago(), false);
        return "admin/metodos-pago";
    }

    @GetMapping("/admin/metodos-pago/editar/{id}")
    public String editarMetodoPago(@PathVariable("id") Long id,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        Optional<MetodoPago> metodoOpt = metodoPagoRepository.findById(id);

        if (metodoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El método de pago solicitado no existe.");
            return "redirect:/admin/metodos-pago";
        }

        prepararVistaMetodosPago(model, metodoOpt.get(), true);
        return "admin/metodos-pago";
    }

    @PostMapping("/admin/metodos-pago/guardar")
    public String guardarMetodoPago(@ModelAttribute("metodoPagoForm") MetodoPago metodoPagoForm,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        boolean esEdicion = metodoPagoForm.getIdMetodoPago() != null;

        if (metodoPagoForm.getTipoTransferencia() == null || metodoPagoForm.getTipoTransferencia().isBlank()) {
            model.addAttribute("error", "El nombre del método de pago es obligatorio.");
            prepararVistaMetodosPago(model, metodoPagoForm, esEdicion);
            return "admin/metodos-pago";
        }

        String tipoNormalizado = metodoPagoForm.getTipoTransferencia().trim().replaceAll("\\s+", " ").toUpperCase();
        metodoPagoForm.setTipoTransferencia(tipoNormalizado);

        if (tipoNormalizado.length() > 40) {
            model.addAttribute("error", "El método de pago no puede superar los 40 caracteres.");
            prepararVistaMetodosPago(model, metodoPagoForm, esEdicion);
            return "admin/metodos-pago";
        }

        if (!METODO_PAGO_PATTERN.matcher(tipoNormalizado).matches()) {
            model.addAttribute("error", "El método de pago solo puede contener letras, números y espacios.");
            prepararVistaMetodosPago(model, metodoPagoForm, esEdicion);
            return "admin/metodos-pago";
        }

        Optional<MetodoPago> duplicadoOpt = metodoPagoRepository.findByTipoTransferenciaIgnoreCase(tipoNormalizado);
        if (duplicadoOpt.isPresent()
                && (metodoPagoForm.getIdMetodoPago() == null
                || !duplicadoOpt.get().getIdMetodoPago().equals(metodoPagoForm.getIdMetodoPago()))) {
            model.addAttribute("error", "Ya existe un método de pago registrado con ese nombre.");
            prepararVistaMetodosPago(model, metodoPagoForm, esEdicion);
            return "admin/metodos-pago";
        }

        MetodoPago metodoGuardar;

        if (esEdicion) {
            Optional<MetodoPago> metodoOpt = metodoPagoRepository.findById(metodoPagoForm.getIdMetodoPago());
            if (metodoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el método de pago a editar.");
                return "redirect:/admin/metodos-pago";
            }
            metodoGuardar = metodoOpt.get();
        } else {
            metodoGuardar = new MetodoPago();
        }

        metodoGuardar.setTipoTransferencia(tipoNormalizado);

        try {
            metodoPagoRepository.save(metodoGuardar);
        } catch (DataIntegrityViolationException ex) {
            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            String mensaje = "No se pudo guardar el método de pago por una restricción de base de datos.";

            if (detalle != null && detalle.toUpperCase().contains("UNIQUE")) {
                mensaje = "Ya existe un método de pago registrado con ese nombre.";
            }

            model.addAttribute("error", mensaje);
            prepararVistaMetodosPago(model, metodoPagoForm, esEdicion);
            return "admin/metodos-pago";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el método de pago.");
            prepararVistaMetodosPago(model, metodoPagoForm, esEdicion);
            return "admin/metodos-pago";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                esEdicion ? "Método de pago actualizado correctamente." : "Método de pago creado correctamente."
        );

        return "redirect:/admin/metodos-pago";
    }

    @PostMapping("/admin/metodos-pago/eliminar/{id}")
    public String eliminarMetodoPago(@PathVariable("id") Long id,
                                     RedirectAttributes redirectAttributes) {

        Optional<MetodoPago> metodoOpt = metodoPagoRepository.findById(id);

        if (metodoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El método de pago no existe.");
            return "redirect:/admin/metodos-pago";
        }

        try {
            metodoPagoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Método de pago eliminado correctamente.");
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el método de pago porque tiene relaciones activas en el sistema."
            );
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al intentar eliminar el método de pago."
            );
        }

        return "redirect:/admin/metodos-pago";
    }

    private void prepararVistaMetodosPago(Model model, MetodoPago metodoPagoForm, boolean modoEdicion) {
        model.addAttribute("metodosPagoListado", metodoPagoRepository.findAll().stream()
                .sorted(Comparator.comparing(MetodoPago::getTipoTransferencia, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("metodoPagoForm", metodoPagoForm);
        model.addAttribute("modoEdicionMetodoPago", modoEdicion);
    }
}