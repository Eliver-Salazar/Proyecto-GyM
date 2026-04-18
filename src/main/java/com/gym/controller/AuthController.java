package com.gym.controller;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/postLogin")
    public String postLogin(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        boolean esAdmin = authorities.stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        boolean esPersonal = authorities.stream()
                .anyMatch(a -> "ROLE_PERSONAL".equals(a.getAuthority()));

        boolean esCliente = authorities.stream()
                .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));

        if (esAdmin) {
            return "redirect:/admin/dashboard";
        }

        if (esPersonal) {
            return "redirect:/personal/panel";
        }

        if (esCliente) {
            return "redirect:/perfil";
        }

        return "redirect:/";
    }
}