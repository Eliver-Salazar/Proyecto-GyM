package com.gym.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IndexController {

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/inscribete")
    public String inscribete() {
        return "redirect:/login";
    }

    @GetMapping("/nosotros")
    public String nosotros() {
        return "nosotros";
    }

    @GetMapping("/faq")
    public String faq() {
        return "PreguntasFrecuentes";
    }

    @GetMapping("/contacto")
    public String contacto(Model model) {
        if (!model.containsAttribute("mensaje")) {
            model.addAttribute("mensaje", null);
        }
        if (!model.containsAttribute("error")) {
            model.addAttribute("error", null);
        }
        return "Contacto";
    }

    @PostMapping("/contacto/enviar")
    public String enviarContacto(@RequestParam("nombre") String nombre,
                                 @RequestParam("correo") String correo,
                                 @RequestParam("mensajeContacto") String mensajeContacto,
                                 RedirectAttributes redirectAttributes) {

        if (nombre == null || nombre.isBlank()
                || correo == null || correo.isBlank()
                || mensajeContacto == null || mensajeContacto.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Todos los campos del formulario de contacto son obligatorios.");
            return "redirect:/contacto";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                "Mensaje enviado correctamente. Gracias por contactarnos, " + nombre.trim() + "."
        );

        return "redirect:/contacto";
    }

    @GetMapping("/horarios")
    public String horarios() {
        return "Horarios";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}