package com.gym.controller;

import com.gym.Repository.ParametroSistemaRepository;
import com.gym.domain.ParametroSistema;
import com.gym.service.oracle.OraclePackageException;
import com.gym.service.oracle.OracleParametroService;
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
public class AdminParametroSistemaController {

    private static final Pattern CLAVE_PATTERN = Pattern.compile("^[A-Z0-9_]+$");

    private final ParametroSistemaRepository parametroSistemaRepository;
    private final OracleParametroService oracleParametroService;

    public AdminParametroSistemaController(ParametroSistemaRepository parametroSistemaRepository,
                                           OracleParametroService oracleParametroService) {
        this.parametroSistemaRepository = parametroSistemaRepository;
        this.oracleParametroService = oracleParametroService;
    }

    @GetMapping("/admin/parametros-sistema")
    public String listarParametros(Model model) {
        cargarVista(model, nuevoParametro(), false);
        return "admin/parametros-sistema";
    }

    @GetMapping("/admin/parametros-sistema/editar/{id}")
    public String editarParametro(@PathVariable("id") Long id,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        var parametroOpt = parametroSistemaRepository.findById(id);

        if (parametroOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El parámetro solicitado no existe.");
            return "redirect:/admin/parametros-sistema";
        }

        cargarVista(model, parametroOpt.get(), true);
        return "admin/parametros-sistema";
    }

    @PostMapping("/admin/parametros-sistema/guardar")
    public String guardarParametro(@ModelAttribute("parametroForm") ParametroSistema parametroForm,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        boolean modoEdicion = parametroForm.getIdParametro() != null;

        String claveNormalizada = normalizarClave(parametroForm.getClave());
        String valorNormalizado = normalizarTexto(parametroForm.getValor());
        String descripcionNormalizada = normalizarTexto(parametroForm.getDescripcion());

        parametroForm.setClave(claveNormalizada);
        parametroForm.setValor(valorNormalizado);
        parametroForm.setDescripcion(descripcionNormalizada);

        String errorValidacion = validarParametro(parametroForm, modoEdicion);
        if (errorValidacion != null) {
            model.addAttribute("error", errorValidacion);
            cargarVista(model, parametroForm, modoEdicion);
            return "admin/parametros-sistema";
        }

        if (modoEdicion) {
            var parametroOpt = parametroSistemaRepository.findById(parametroForm.getIdParametro());
            if (parametroOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El parámetro a editar ya no existe.");
                return "redirect:/admin/parametros-sistema";
            }
        }

        try {
            if (modoEdicion) {
                oracleParametroService.actualizarParametro(
                        parametroForm.getIdParametro(),
                        claveNormalizada,
                        valorNormalizado,
                        descripcionNormalizada
                );
            } else {
                oracleParametroService.crearParametro(
                        claveNormalizada,
                        valorNormalizado,
                        descripcionNormalizada
                );
            }

        } catch (OraclePackageException ex) {
            model.addAttribute("error", traducirErrorOracle(ex));
            model.addAttribute("errorTecnico", obtenerDetalleTecnico(ex));
            cargarVista(model, parametroForm, modoEdicion);
            return "admin/parametros-sistema";

        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar el parámetro por una restricción de base de datos.");
            model.addAttribute("errorTecnico", obtenerDetalleTecnico(ex));
            cargarVista(model, parametroForm, modoEdicion);
            return "admin/parametros-sistema";

        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el parámetro.");
            model.addAttribute("errorTecnico", ex.getMessage());
            cargarVista(model, parametroForm, modoEdicion);
            return "admin/parametros-sistema";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                modoEdicion ? "Parámetro actualizado correctamente." : "Parámetro creado correctamente."
        );

        return "redirect:/admin/parametros-sistema";
    }

    @PostMapping("/admin/parametros-sistema/eliminar/{id}")
    public String eliminarParametro(@PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {

        var parametroOpt = parametroSistemaRepository.findById(id);

        if (parametroOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El parámetro solicitado no existe.");
            return "redirect:/admin/parametros-sistema";
        }

        try {
            oracleParametroService.eliminarParametro(id);
            redirectAttributes.addFlashAttribute("mensaje", "Parámetro eliminado correctamente.");

        } catch (OraclePackageException ex) {
            redirectAttributes.addFlashAttribute("error", traducirErrorOracle(ex));

        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se pudo eliminar el parámetro por una restricción de integridad."
            );

        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ocurrió un error inesperado al eliminar el parámetro."
            );
        }

        return "redirect:/admin/parametros-sistema";
    }

    private void cargarVista(Model model, ParametroSistema parametroForm, boolean modoEdicion) {
        model.addAttribute("parametroForm", parametroForm);
        model.addAttribute("modoEdicionParametro", modoEdicion);
        model.addAttribute("parametrosListado", parametroSistemaRepository.findAllByOrderByClaveAsc());
    }

    private ParametroSistema nuevoParametro() {
        return new ParametroSistema();
    }

    private String validarParametro(ParametroSistema parametro, boolean modoEdicion) {
        if (parametro.getClave() == null || parametro.getClave().isBlank()) {
            return "La clave es obligatoria.";
        }

        if (parametro.getClave().length() > 50) {
            return "La clave no puede superar los 50 caracteres.";
        }

        if (!CLAVE_PATTERN.matcher(parametro.getClave()).matches()) {
            return "La clave solo puede contener letras mayúsculas, números y guion bajo.";
        }

        if (parametro.getValor() == null || parametro.getValor().isBlank()) {
            return "El valor es obligatorio.";
        }

        if (parametro.getValor().length() > 200) {
            return "El valor no puede superar los 200 caracteres.";
        }

        if (parametro.getDescripcion() != null && parametro.getDescripcion().length() > 200) {
            return "La descripción no puede superar los 200 caracteres.";
        }

        if (!modoEdicion && parametroSistemaRepository.existsByClaveIgnoreCase(parametro.getClave())) {
            return "Ya existe un parámetro con esa clave.";
        }

        if (modoEdicion
                && parametro.getIdParametro() != null
                && parametroSistemaRepository.existsByClaveIgnoreCaseAndIdParametroNot(
                        parametro.getClave(), parametro.getIdParametro())) {
            return "Ya existe otro parámetro con esa clave.";
        }

        return null;
    }

    private String normalizarClave(String clave) {
        if (clave == null) {
            return null;
        }
        return clave.trim().replaceAll("\\s+", "_").toUpperCase();
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        return limpio.isBlank() ? null : limpio;
    }

    private String traducirErrorOracle(Throwable ex) {
        String detalle = obtenerDetalleTecnico(ex);

        if (detalle == null || detalle.isBlank()) {
            return "Ocurrió un error al procesar el parámetro con Oracle.";
        }

        String upper = detalle.toUpperCase();

        if (upper.contains("ORA-20501")) {
            return "La clave del parámetro es obligatoria.";
        }
        if (upper.contains("ORA-20502")) {
            return "Ya existe un parámetro con esa clave.";
        }
        if (upper.contains("ORA-20503")) {
            return "El parámetro indicado no existe.";
        }
        if (upper.contains("ORA-20504")) {
            return "La clave del parámetro es obligatoria.";
        }
        if (upper.contains("ORA-20505")) {
            return "Ya existe otro parámetro con esa clave.";
        }
        if (upper.contains("ORA-20506")) {
            return "El parámetro indicado no existe.";
        }

        return "Ocurrió un error al procesar el parámetro en Oracle.";
    }

    private String obtenerDetalleTecnico(Throwable ex) {
        Throwable causa = ex;

        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }

        return causa.getMessage() != null
                ? causa.getMessage()
                : ex.getMessage();
    }
}