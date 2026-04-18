package com.gym.controller;

import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.RolRepository;
import com.gym.Repository.SucursalRepository;
import com.gym.Repository.TipoMembresiaRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Membresia;
import com.gym.domain.Rol;
import com.gym.domain.TipoMembresia;
import com.gym.domain.Usuario;
import com.gym.service.UsuarioService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final TipoMembresiaRepository tipoMembresiaRepository;
    private final PagoRepository pagoRepository;
    private final SucursalRepository sucursalRepository;
    private final RolRepository rolRepository;
    private final UsuarioService usuarioService;

    public AdminController(UsuarioRepository usuarioRepository,
                           MembresiaRepository membresiaRepository,
                           TipoMembresiaRepository tipoMembresiaRepository,
                           PagoRepository pagoRepository,
                           SucursalRepository sucursalRepository,
                           RolRepository rolRepository,
                           UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.tipoMembresiaRepository = tipoMembresiaRepository;
        this.pagoRepository = pagoRepository;
        this.sucursalRepository = sucursalRepository;
        this.rolRepository = rolRepository;
        this.usuarioService = usuarioService;
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

    @GetMapping("/admin/usuarios")
    public String usuarios(Model model) {
        prepararVistaUsuarios(model, new Usuario(), false);
        return "admin/usuarios";
    }

    @GetMapping("/admin/usuarios/editar/{id}")
    public String editarUsuario(@PathVariable("id") Long id,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El usuario solicitado no existe.");
            return "redirect:/admin/usuarios";
        }

        prepararVistaUsuarios(model, usuarioOpt.get(), true);
        return "admin/usuarios";
    }

    @PostMapping("/admin/usuarios/guardar")
    public String guardarUsuario(@ModelAttribute("usuarioForm") Usuario usuarioForm,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        if (usuarioForm.getNombre() == null || usuarioForm.getNombre().isBlank()
                || usuarioForm.getApellido() == null || usuarioForm.getApellido().isBlank()
                || usuarioForm.getCorreo() == null || usuarioForm.getCorreo().isBlank()
                || usuarioForm.getTelefono() == null || usuarioForm.getTelefono().isBlank()
                || usuarioForm.getCedula() == null || usuarioForm.getCedula().isBlank()
                || usuarioForm.getFechaNacimiento() == null
                || usuarioForm.getTelefonoEmergencia() == null || usuarioForm.getTelefonoEmergencia().isBlank()
                || usuarioForm.getRol() == null || usuarioForm.getRol().getIdRol() == null) {

            model.addAttribute("error",
                    "Debes completar nombre, apellido, correo, teléfono, cédula, fecha de nacimiento, teléfono de emergencia y rol.");
            prepararVistaUsuarios(model, usuarioForm, usuarioForm.getIdUsuario() != null);
            return "admin/usuarios";
        }

        Optional<Rol> rolOpt = rolRepository.findById(usuarioForm.getRol().getIdRol());
        if (rolOpt.isEmpty()) {
            model.addAttribute("error", "El rol seleccionado no existe.");
            prepararVistaUsuarios(model, usuarioForm, usuarioForm.getIdUsuario() != null);
            return "admin/usuarios";
        }

        String telefonoNormalizado = limpiarTelefono(usuarioForm.getTelefono());
        String telefonoEmergenciaNormalizado = limpiarTelefono(usuarioForm.getTelefonoEmergencia());

        if (telefonoNormalizado == null || !telefonoNormalizado.matches("\\d{8}")) {
            model.addAttribute("error", "El teléfono debe contener exactamente 8 dígitos.");
            prepararVistaUsuarios(model, usuarioForm, usuarioForm.getIdUsuario() != null);
            return "admin/usuarios";
        }

        if (telefonoEmergenciaNormalizado == null || !telefonoEmergenciaNormalizado.matches("\\d{8}")) {
            model.addAttribute("error", "El teléfono de emergencia debe contener exactamente 8 dígitos.");
            prepararVistaUsuarios(model, usuarioForm, usuarioForm.getIdUsuario() != null);
            return "admin/usuarios";
        }

        boolean esEdicion = usuarioForm.getIdUsuario() != null;
        Usuario usuarioGuardar;

        if (esEdicion) {
            Optional<Usuario> existenteOpt = usuarioRepository.findById(usuarioForm.getIdUsuario());
            if (existenteOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el usuario a editar.");
                return "redirect:/admin/usuarios";
            }
            usuarioGuardar = existenteOpt.get();
        } else {
            usuarioGuardar = new Usuario();
            usuarioGuardar.setFotoPerfil(new byte[]{0});
        }

        usuarioGuardar.setNombre(usuarioForm.getNombre());
        usuarioGuardar.setApellido(usuarioForm.getApellido());
        usuarioGuardar.setCorreo(usuarioForm.getCorreo());
        usuarioGuardar.setTelefono(telefonoNormalizado);
        usuarioGuardar.setCedula(usuarioForm.getCedula());
        usuarioGuardar.setFechaNacimiento(usuarioForm.getFechaNacimiento());
        usuarioGuardar.setContactoEmergencia(usuarioForm.getContactoEmergencia());
        usuarioGuardar.setTelefonoEmergencia(telefonoEmergenciaNormalizado);
        usuarioGuardar.setCondicionMedica(usuarioForm.getCondicionMedica());
        usuarioGuardar.setRol(rolOpt.get());

        if (!esEdicion) {
            String password = (usuarioForm.getContrasenia() == null || usuarioForm.getContrasenia().isBlank())
                    ? "1234"
                    : usuarioForm.getContrasenia();
            usuarioGuardar.setContrasenia(password);
        } else if (usuarioForm.getContrasenia() != null && !usuarioForm.getContrasenia().isBlank()) {
            usuarioGuardar.setContrasenia(usuarioForm.getContrasenia());
        }

        try {
            usuarioService.guardarUsuario(usuarioGuardar);
        } catch (DataIntegrityViolationException ex) {
            String mensaje = "No se pudo guardar el usuario por una restricción de base de datos.";

            String detalle = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();

            if (detalle != null) {
                String detalleUpper = detalle.toUpperCase();

                if (detalleUpper.contains("USUARIOS_TEL_EMERG_CHK")) {
                    mensaje = "El teléfono de emergencia no cumple el formato requerido por la base de datos.";
                } else if (detalleUpper.contains("CORREO")) {
                    mensaje = "Ya existe un usuario registrado con ese correo.";
                } else if (detalleUpper.contains("CEDULA")) {
                    mensaje = "Ya existe un usuario registrado con esa cédula.";
                }
            }

            model.addAttribute("error", mensaje);
            prepararVistaUsuarios(model, usuarioForm, esEdicion);
            return "admin/usuarios";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el usuario.");
            prepararVistaUsuarios(model, usuarioForm, esEdicion);
            return "admin/usuarios";
        }

        redirectAttributes.addFlashAttribute("mensaje",
                esEdicion ? "Usuario actualizado correctamente." : "Usuario creado correctamente.");

        return "redirect:/admin/usuarios";
    }

    @PostMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Long id,
                                  RedirectAttributes redirectAttributes) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El usuario no existe.");
            return "redirect:/admin/usuarios";
        }

        try {
            usuarioRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo eliminar el usuario porque tiene relaciones activas en el sistema.");
        }

        return "redirect:/admin/usuarios";
    }

    @GetMapping("/admin/roles")
    public String roles(Model model) {
        prepararVistaRoles(model, new Rol(), false);
        return "admin/roles";
    }

    @GetMapping("/admin/roles/editar/{id}")
    public String editarRol(@PathVariable("id") Long id,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        Optional<Rol> rolOpt = rolRepository.findById(id);

        if (rolOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El rol solicitado no existe.");
            return "redirect:/admin/roles";
        }

        prepararVistaRoles(model, rolOpt.get(), true);
        return "admin/roles";
    }

    @PostMapping("/admin/roles/guardar")
    public String guardarRol(@ModelAttribute("rolForm") Rol rolForm,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (rolForm.getNombre() == null || rolForm.getNombre().isBlank()) {
            model.addAttribute("error", "El nombre del rol es obligatorio.");
            prepararVistaRoles(model, rolForm, rolForm.getIdRol() != null);
            return "admin/roles";
        }

        String nombreNormalizado = rolForm.getNombre().trim().toUpperCase();

        Optional<Rol> rolExistenteNombre = rolRepository.findByNombre(nombreNormalizado);

        if (rolExistenteNombre.isPresent()
                && (rolForm.getIdRol() == null || !rolExistenteNombre.get().getIdRol().equals(rolForm.getIdRol()))) {
            model.addAttribute("error", "Ya existe un rol registrado con ese nombre.");
            rolForm.setNombre(nombreNormalizado);
            prepararVistaRoles(model, rolForm, rolForm.getIdRol() != null);
            return "admin/roles";
        }

        boolean esEdicion = rolForm.getIdRol() != null;
        Rol rolGuardar;

        if (esEdicion) {
            Optional<Rol> rolOpt = rolRepository.findById(rolForm.getIdRol());
            if (rolOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el rol a editar.");
                return "redirect:/admin/roles";
            }
            rolGuardar = rolOpt.get();
        } else {
            rolGuardar = new Rol();
        }

        rolGuardar.setNombre(nombreNormalizado);

        try {
            rolRepository.save(rolGuardar);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", "No se pudo guardar el rol porque ya existe o viola una restricción.");
            prepararVistaRoles(model, rolForm, esEdicion);
            return "admin/roles";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el rol.");
            prepararVistaRoles(model, rolForm, esEdicion);
            return "admin/roles";
        }

        redirectAttributes.addFlashAttribute("mensaje",
                esEdicion ? "Rol actualizado correctamente." : "Rol creado correctamente.");

        return "redirect:/admin/roles";
    }

    @PostMapping("/admin/roles/eliminar/{id}")
    public String eliminarRol(@PathVariable("id") Long id,
                              RedirectAttributes redirectAttributes) {

        Optional<Rol> rolOpt = rolRepository.findById(id);

        if (rolOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El rol no existe.");
            return "redirect:/admin/roles";
        }

        try {
            rolRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Rol eliminado correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo eliminar el rol porque está siendo utilizado por uno o más usuarios.");
        }

        return "redirect:/admin/roles";
    }

    @GetMapping("/admin/tipos-membresia")
    public String tiposMembresia(Model model) {
        prepararVistaTiposMembresia(model, new TipoMembresia(), false);
        return "admin/tipos-membresia";
    }

    @GetMapping("/admin/tipos-membresia/editar/{id}")
    public String editarTipoMembresia(@PathVariable("id") Long id,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {

        Optional<TipoMembresia> tipoOpt = tipoMembresiaRepository.findById(id);

        if (tipoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El tipo de membresía solicitado no existe.");
            return "redirect:/admin/tipos-membresia";
        }

        prepararVistaTiposMembresia(model, tipoOpt.get(), true);
        return "admin/tipos-membresia";
    }

    @PostMapping("/admin/tipos-membresia/guardar")
    public String guardarTipoMembresia(@ModelAttribute("tipoForm") TipoMembresia tipoForm,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        if (tipoForm.getNombre() == null || tipoForm.getNombre().isBlank()
                || tipoForm.getCantidadDias() == null
                || tipoForm.getPrecio() == null
                || tipoForm.getAccesoGlobal() == null || tipoForm.getAccesoGlobal().isBlank()) {

            model.addAttribute("error",
                    "Debes completar nombre, cantidad de días, precio y acceso global.");
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        String nombreNormalizado = tipoForm.getNombre().trim().toUpperCase();
        String accesoGlobalNormalizado = tipoForm.getAccesoGlobal().trim().toUpperCase();

        if (tipoForm.getCantidadDias() <= 0) {
            model.addAttribute("error", "La cantidad de días debe ser mayor que cero.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        if (tipoForm.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "El precio no puede ser negativo.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        if (!accesoGlobalNormalizado.equals("S") && !accesoGlobalNormalizado.equals("N")) {
            model.addAttribute("error", "El acceso global debe ser 'S' o 'N'.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        List<TipoMembresia> existentes = tipoMembresiaRepository.findAll();
        boolean nombreDuplicado = existentes.stream()
                .anyMatch(t -> t.getNombre() != null
                        && t.getNombre().equalsIgnoreCase(nombreNormalizado)
                        && (tipoForm.getIdTipoMembresia() == null
                        || !t.getIdTipoMembresia().equals(tipoForm.getIdTipoMembresia())));

        if (nombreDuplicado) {
            model.addAttribute("error", "Ya existe un tipo de membresía registrado con ese nombre.");
            tipoForm.setNombre(nombreNormalizado);
            tipoForm.setAccesoGlobal(accesoGlobalNormalizado);
            prepararVistaTiposMembresia(model, tipoForm, tipoForm.getIdTipoMembresia() != null);
            return "admin/tipos-membresia";
        }

        boolean esEdicion = tipoForm.getIdTipoMembresia() != null;
        TipoMembresia tipoGuardar;

        if (esEdicion) {
            Optional<TipoMembresia> tipoOpt = tipoMembresiaRepository.findById(tipoForm.getIdTipoMembresia());
            if (tipoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No se encontró el tipo de membresía a editar.");
                return "redirect:/admin/tipos-membresia";
            }
            tipoGuardar = tipoOpt.get();
        } else {
            tipoGuardar = new TipoMembresia();
        }

        tipoGuardar.setNombre(nombreNormalizado);
        tipoGuardar.setCantidadDias(tipoForm.getCantidadDias());
        tipoGuardar.setPrecio(tipoForm.getPrecio());
        tipoGuardar.setAccesoGlobal(accesoGlobalNormalizado);

        try {
            tipoMembresiaRepository.save(tipoGuardar);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error",
                    "No se pudo guardar el tipo de membresía porque ya existe o viola una restricción.");
            prepararVistaTiposMembresia(model, tipoForm, esEdicion);
            return "admin/tipos-membresia";
        } catch (Exception ex) {
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el tipo de membresía.");
            prepararVistaTiposMembresia(model, tipoForm, esEdicion);
            return "admin/tipos-membresia";
        }

        redirectAttributes.addFlashAttribute("mensaje",
                esEdicion ? "Tipo de membresía actualizado correctamente."
                          : "Tipo de membresía creado correctamente.");

        return "redirect:/admin/tipos-membresia";
    }

    @PostMapping("/admin/tipos-membresia/eliminar/{id}")
    public String eliminarTipoMembresia(@PathVariable("id") Long id,
                                        RedirectAttributes redirectAttributes) {

        Optional<TipoMembresia> tipoOpt = tipoMembresiaRepository.findById(id);

        if (tipoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El tipo de membresía no existe.");
            return "redirect:/admin/tipos-membresia";
        }

        try {
            tipoMembresiaRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensaje", "Tipo de membresía eliminado correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo eliminar el tipo de membresía porque está siendo utilizado por una o más membresías.");
        }

        return "redirect:/admin/tipos-membresia";
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

    private void prepararVistaUsuarios(Model model, Usuario usuarioForm, boolean modoEdicion) {
        model.addAttribute("usuarios", usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getIdUsuario, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList());
        model.addAttribute("roles", rolRepository.findAll().stream()
                .sorted(Comparator.comparing(Rol::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("usuarioForm", usuarioForm);
        model.addAttribute("modoEdicion", modoEdicion);
    }

    private void prepararVistaRoles(Model model, Rol rolForm, boolean modoEdicion) {
        model.addAttribute("rolesListado", rolRepository.findAll().stream()
                .sorted(Comparator.comparing(Rol::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("rolForm", rolForm);
        model.addAttribute("modoEdicionRol", modoEdicion);
    }

    private void prepararVistaTiposMembresia(Model model, TipoMembresia tipoForm, boolean modoEdicion) {
        model.addAttribute("tiposListado", tipoMembresiaRepository.findAll().stream()
                .sorted(Comparator.comparing(TipoMembresia::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
        model.addAttribute("tipoForm", tipoForm);
        model.addAttribute("modoEdicionTipo", modoEdicion);
    }

    private String limpiarTelefono(String telefono) {
        if (telefono == null) {
            return null;
        }
        return telefono.replaceAll("[^0-9]", "").trim();
    }
}