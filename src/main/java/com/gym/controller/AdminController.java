package com.gym.controller;

import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.RolRepository;
import com.gym.Repository.SucursalRepository;
import com.gym.Repository.TipoMembresiaRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Membresia;
import com.gym.domain.Usuario;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final TipoMembresiaRepository tipoMembresiaRepository;
    private final PagoRepository pagoRepository;
    private final SucursalRepository sucursalRepository;
    private final RolRepository rolRepository;

    public AdminController(UsuarioRepository usuarioRepository,
                           MembresiaRepository membresiaRepository,
                           TipoMembresiaRepository tipoMembresiaRepository,
                           PagoRepository pagoRepository,
                           SucursalRepository sucursalRepository,
                           RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.tipoMembresiaRepository = tipoMembresiaRepository;
        this.pagoRepository = pagoRepository;
        this.sucursalRepository = sucursalRepository;
        this.rolRepository = rolRepository;
    }

    @GetMapping({"/admin", "/admin/", "/admin/dashboard"})
    public String adminIndex(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Membresia> membresias = membresiaRepository.findAll();

        long totalUsuarios = usuarios.size();
        long totalClientes = usuarios.stream()
                .filter(u -> u.getRol() != null && "CLIENTE".equalsIgnoreCase(u.getRol().getNombre()))
                .count();

        long totalPersonal = usuarios.stream()
                .filter(u -> u.getRol() != null && "PERSONAL".equalsIgnoreCase(u.getRol().getNombre()))
                .count();

        long totalAdmins = usuarios.stream()
                .filter(u -> u.getRol() != null && "ADMIN".equalsIgnoreCase(u.getRol().getNombre()))
                .count();

        long totalMembresias = membresias.size();
        long totalTipos = tipoMembresiaRepository.count();
        long totalPagos = pagoRepository.count();
        long totalSucursales = sucursalRepository.count();
        long totalRoles = rolRepository.count();

        List<Usuario> usuariosRecientes = usuarios.stream()
                .sorted(Comparator.comparing(Usuario::getIdUsuario, Comparator.nullsLast(Long::compareTo)).reversed())
                .limit(5)
                .toList();

        List<Membresia> membresiasRecientes = membresias.stream()
                .sorted(Comparator.comparing(Membresia::getIdMembresia, Comparator.nullsLast(Long::compareTo)).reversed())
                .limit(5)
                .toList();

        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalPersonal", totalPersonal);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("totalMembresias", totalMembresias);
        model.addAttribute("totalTipos", totalTipos);
        model.addAttribute("totalPagos", totalPagos);
        model.addAttribute("totalSucursales", totalSucursales);
        model.addAttribute("totalRoles", totalRoles);
        model.addAttribute("usuariosRecientes", usuariosRecientes);
        model.addAttribute("membresiasRecientes", membresiasRecientes);

        return "admin/index";
    }
}