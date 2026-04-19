package com.gym.controller;

import com.gym.Repository.BitacoraAccesoRepository;
import com.gym.Repository.SucursalRepository;
import com.gym.domain.BitacoraAcceso;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminBitacoraAccesoController {

    private final BitacoraAccesoRepository bitacoraAccesoRepository;
    private final SucursalRepository sucursalRepository;

    public AdminBitacoraAccesoController(BitacoraAccesoRepository bitacoraAccesoRepository,
                                         SucursalRepository sucursalRepository) {
        this.bitacoraAccesoRepository = bitacoraAccesoRepository;
        this.sucursalRepository = sucursalRepository;
    }

    @GetMapping("/admin/bitacora-acceso")
    public String verBitacoraAcceso(
            @RequestParam(name = "textoUsuario", required = false) String textoUsuario,
            @RequestParam(name = "idSucursal", required = false) Long idSucursal,
            @RequestParam(name = "resultado", required = false) String resultado,
            @RequestParam(name = "fechaDesde", required = false) String fechaDesde,
            @RequestParam(name = "fechaHasta", required = false) String fechaHasta,
            Model model) {

        String textoUsuarioNormalizado = normalizarTexto(textoUsuario);
        String resultadoNormalizado = normalizarTexto(resultado);

        LocalDate fechaDesdeLocal = parsearFecha(fechaDesde);
        LocalDate fechaHastaLocal = parsearFecha(fechaHasta);

        if (fechaDesde != null && !fechaDesde.isBlank() && fechaDesdeLocal == null) {
            model.addAttribute("error", "La fecha desde no tiene un formato válido.");
        }

        if (fechaHasta != null && !fechaHasta.isBlank() && fechaHastaLocal == null) {
            model.addAttribute("error", "La fecha hasta no tiene un formato válido.");
        }

        if (fechaDesdeLocal != null && fechaHastaLocal != null && fechaDesdeLocal.isAfter(fechaHastaLocal)) {
            model.addAttribute("error", "La fecha desde no puede ser mayor que la fecha hasta.");
            fechaDesdeLocal = null;
            fechaHastaLocal = null;
        }

        Date fechaDesdeDate = convertirInicioDia(fechaDesdeLocal);
        Date fechaHastaDate = convertirFinDia(fechaHastaLocal);

        List<BitacoraAcceso> registros = bitacoraAccesoRepository.filtrarBitacora(
                textoUsuarioNormalizado,
                idSucursal,
                resultadoNormalizado,
                fechaDesdeDate,
                fechaHastaDate
        );

        model.addAttribute("bitacoraListado", registros);
        model.addAttribute("sucursalesDisponibles", sucursalRepository.findAll());
        model.addAttribute("resultadosDisponibles", bitacoraAccesoRepository.listarResultadosDisponibles());

        model.addAttribute("textoUsuario", textoUsuarioNormalizado == null ? "" : textoUsuarioNormalizado);
        model.addAttribute("idSucursal", idSucursal);
        model.addAttribute("resultado", resultadoNormalizado == null ? "" : resultadoNormalizado);
        model.addAttribute("fechaDesde", fechaDesdeLocal != null ? fechaDesdeLocal.toString() : "");
        model.addAttribute("fechaHasta", fechaHastaLocal != null ? fechaHastaLocal.toString() : "");

        return "admin/bitacora-acceso";
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim().replaceAll("\\s+", " ");
        return limpio.isBlank() ? null : limpio;
    }

    private LocalDate parsearFecha(String fecha) {
        try {
            if (fecha == null || fecha.isBlank()) {
                return null;
            }
            return LocalDate.parse(fecha);
        } catch (Exception ex) {
            return null;
        }
    }

    private Date convertirInicioDia(LocalDate fecha) {
        if (fecha == null) {
            return null;
        }
        return Date.from(
                fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );
    }

    private Date convertirFinDia(LocalDate fecha) {
        if (fecha == null) {
            return null;
        }
        return Date.from(
                fecha.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}