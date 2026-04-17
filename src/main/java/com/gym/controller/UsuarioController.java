package com.gym.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping
    public String listar() {
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/nuevo")
    public String nuevo() {
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/guardar")
    public String guardar() {
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id) {
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id) {
        return "redirect:/admin/usuarios";
    }
}