package com.gym.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/membresias")
public class MembresiaController {

    @GetMapping
    public String listar() {
        return "redirect:/admin/membresias";
    }

    @GetMapping("/nueva")
    public String nueva() {
        return "redirect:/admin/membresias";
    }

    @PostMapping("/guardar")
    public String guardar() {
        return "redirect:/admin/membresias";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id) {
        return "redirect:/admin/membresias";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id) {
        return "redirect:/admin/membresias";
    }
}