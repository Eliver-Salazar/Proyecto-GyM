package com.gym.controller;

import com.gym.Repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PersonalController {

    private final UsuarioRepository usuarioRepository;

    public PersonalController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"/personal", "/personal/", "/personal/panel"})
    public String panel() {
        return "personal/panel";
    }

    @GetMapping("/personal/turnos")
    public String turnos() {
        return "personal/turnos";
    }

    @GetMapping("/personal/socios")
    public String socios(Model model) {
        model.addAttribute("socios", usuarioRepository.findAll());
        return "personal/socios";
    }
}