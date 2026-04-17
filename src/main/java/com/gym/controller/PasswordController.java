package com.gym.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordController {

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("mensaje", null);
        model.addAttribute("error", null);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String procesarForgotPassword(@RequestParam("correo") String correo,
                                         RedirectAttributes redirectAttributes) {

        if (correo == null || correo.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Debes ingresar un correo para continuar.");
            return "redirect:/forgot-password";
        }

        redirectAttributes.addFlashAttribute(
                "mensaje",
                "Si el correo existe en el sistema, se procesará la solicitud de recuperación."
        );

        return "redirect:/forgot-password/done";
    }

    @GetMapping("/forgot-password/done")
    public String forgotPasswordDone() {
        return "auth/forgot-password-done";
    }
}