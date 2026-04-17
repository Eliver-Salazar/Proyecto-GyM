package com.gym.controller;

import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.TipoMembresiaRepository;
import com.gym.Repository.UsuarioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final TipoMembresiaRepository tipoMembresiaRepository;

    public AdminController(UsuarioRepository usuarioRepository,
                           MembresiaRepository membresiaRepository,
                           TipoMembresiaRepository tipoMembresiaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.tipoMembresiaRepository = tipoMembresiaRepository;
    }

    @GetMapping({"/admin", "/admin/"})
    public String adminIndex() {
        return "admin/index";
    }

    @GetMapping("/admin/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "admin/usuarios";
    }

    @GetMapping("/admin/membresias")
    public String membresias(Model model) {
        model.addAttribute("membresias", membresiaRepository.findAll());
        model.addAttribute("tiposMembresia", tipoMembresiaRepository.findAll());
        return "admin/membresias";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("membresias", membresiaRepository.findAll());
        model.addAttribute("tiposMembresia", tipoMembresiaRepository.findAll());
        return "admin/reportes";
    }
}