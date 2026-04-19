package com.gym.controller;

import com.gym.Repository.MembresiaRepository;
import com.gym.Repository.PagoRepository;
import com.gym.Repository.UsuarioRepository;
import com.gym.domain.Membresia;
import com.gym.domain.Pago;
import com.gym.domain.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminReporteController {

    private final UsuarioRepository usuarioRepository;
    private final MembresiaRepository membresiaRepository;
    private final PagoRepository pagoRepository;

    public AdminReporteController(UsuarioRepository usuarioRepository,
            MembresiaRepository membresiaRepository,
            PagoRepository pagoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.membresiaRepository = membresiaRepository;
        this.pagoRepository = pagoRepository;
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model) {

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Membresia> membresias = membresiaRepository.findAll();
        List<Pago> pagos = pagoRepository.findAll();

        long totalClientes = usuarios.stream()
                .filter(this::esCliente)
                .count();

        long membresiasActivas = membresias.stream()
                .filter(this::esMembresiaActiva)
                .count();

        BigDecimal ingresosTotales = pagos.stream()
                .filter(this::esPagoActivo)
                .map(Pago::getMonto)
                .filter(monto -> monto != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate hoy = LocalDate.now();
        long pagosMesActual = pagos.stream()
                .filter(this::esPagoActivo)
                .filter(p -> p.getFechaPago() != null)
                .map(p -> convertirLocalDate(p.getFechaPago()))
                .filter(fecha -> fecha != null
                && fecha.getYear() == hoy.getYear()
                && fecha.getMonthValue() == hoy.getMonthValue())
                .count();

        Map<String, Long> resumenEstados = construirResumenEstados(membresias);

        List<ReporteVencimientoItem> proximosVencimientos = membresias.stream()
                .filter(m -> m.getFechaVencimiento() != null)
                .sorted(Comparator.comparing(Membresia::getFechaVencimiento))
                .limit(10)
                .map(this::mapearVencimiento)
                .toList();

        List<ReportePagoItem> pagosRecientes = pagos.stream()
                .sorted(Comparator
                        .comparing(Pago::getFechaPago, Comparator.nullsLast(Date::compareTo))
                        .reversed()
                        .thenComparing(Pago::getIdPago, Comparator.nullsLast(Long::compareTo)).reversed())
                .limit(10)
                .map(this::mapearPago)
                .toList();

        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("membresiasActivas", membresiasActivas);
        model.addAttribute("ingresosTotales", ingresosTotales);
        model.addAttribute("pagosMesActual", pagosMesActual);

        model.addAttribute("resumenEstados", resumenEstados);
        model.addAttribute("proximosVencimientos", proximosVencimientos);
        model.addAttribute("pagosRecientes", pagosRecientes);

        return "admin/reportes";
    }

    private boolean esCliente(Usuario usuario) {
        return usuario != null
                && usuario.getRol() != null
                && usuario.getRol().getNombre() != null
                && "CLIENTE".equalsIgnoreCase(usuario.getRol().getNombre().trim());
    }

    private boolean esMembresiaActiva(Membresia membresia) {
        return membresia != null
                && membresia.getEstadoMembresia() != null
                && membresia.getEstadoMembresia().getNombreEstado() != null
                && "ACTIVA".equalsIgnoreCase(membresia.getEstadoMembresia().getNombreEstado().trim());
    }

    private boolean esPagoActivo(Pago pago) {
        return pago != null
                && pago.getActivo() != null
                && "S".equalsIgnoreCase(pago.getActivo().trim());
    }

    private Map<String, Long> construirResumenEstados(List<Membresia> membresias) {
        Map<String, Long> resumen = new LinkedHashMap<>();

        membresias.stream()
                .map(m -> {
                    if (m == null || m.getEstadoMembresia() == null || m.getEstadoMembresia().getNombreEstado() == null) {
                        return "SIN ESTADO";
                    }
                    return m.getEstadoMembresia().getNombreEstado().trim().toUpperCase();
                })
                .sorted()
                .forEach(estado -> resumen.put(estado, resumen.getOrDefault(estado, 0L) + 1));

        return resumen;
    }

    private ReporteVencimientoItem mapearVencimiento(Membresia membresia) {
        String cliente = "Sin cliente";
        String tipo = "Sin tipo";
        String estado = "Sin estado";
        Long diasRestantes = null;

        if (membresia.getUsuario() != null) {
            String nombre = membresia.getUsuario().getNombre() != null ? membresia.getUsuario().getNombre().trim() : "";
            String apellido = membresia.getUsuario().getApellido() != null ? membresia.getUsuario().getApellido().trim() : "";
            String nombreCompleto = (nombre + " " + apellido).trim();
            if (!nombreCompleto.isBlank()) {
                cliente = nombreCompleto;
            }
        }

        if (membresia.getTipoMembresia() != null && membresia.getTipoMembresia().getNombre() != null) {
            tipo = membresia.getTipoMembresia().getNombre().trim();
        }

        if (membresia.getEstadoMembresia() != null && membresia.getEstadoMembresia().getNombreEstado() != null) {
            estado = membresia.getEstadoMembresia().getNombreEstado().trim();
        }

        LocalDate fechaVencimiento = convertirLocalDate(membresia.getFechaVencimiento());
        if (fechaVencimiento != null) {
            diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
        }

        return new ReporteVencimientoItem(
                membresia.getIdMembresia(),
                cliente,
                tipo,
                estado,
                membresia.getFechaVencimiento(),
                diasRestantes
        );
    }

    private ReportePagoItem mapearPago(Pago pago) {
        String cliente = "Sin cliente";
        String tipo = "Sin membresía";
        String metodo = "Sin método";
        String activoTexto = "No";

        if (pago.getUsuario() != null) {
            String nombre = pago.getUsuario().getNombre() != null ? pago.getUsuario().getNombre().trim() : "";
            String apellido = pago.getUsuario().getApellido() != null ? pago.getUsuario().getApellido().trim() : "";
            String nombreCompleto = (nombre + " " + apellido).trim();
            if (!nombreCompleto.isBlank()) {
                cliente = nombreCompleto;
            }
        }

        if (pago.getMembresia() != null
                && pago.getMembresia().getTipoMembresia() != null
                && pago.getMembresia().getTipoMembresia().getNombre() != null) {
            tipo = pago.getMembresia().getTipoMembresia().getNombre().trim();
        }

        if (pago.getMetodoPago() != null && pago.getMetodoPago().getTipoTransferencia() != null) {
            metodo = pago.getMetodoPago().getTipoTransferencia().trim();
        }

        if (esPagoActivo(pago)) {
            activoTexto = "Sí";
        }

        return new ReportePagoItem(
                pago.getIdPago(),
                cliente,
                tipo,
                metodo,
                pago.getMonto(),
                pago.getFechaPago(),
                activoTexto
        );
    }

    private LocalDate convertirLocalDate(Date fecha) {
        if (fecha == null) {
            return null;
        }

        if (fecha instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return fecha.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static class ReporteVencimientoItem {

        private final Long idMembresia;
        private final String cliente;
        private final String tipoMembresia;
        private final String estado;
        private final Date fechaVencimiento;
        private final Long diasRestantes;

        public ReporteVencimientoItem(Long idMembresia,
                String cliente,
                String tipoMembresia,
                String estado,
                Date fechaVencimiento,
                Long diasRestantes) {
            this.idMembresia = idMembresia;
            this.cliente = cliente;
            this.tipoMembresia = tipoMembresia;
            this.estado = estado;
            this.fechaVencimiento = fechaVencimiento;
            this.diasRestantes = diasRestantes;
        }

        public Long getIdMembresia() {
            return idMembresia;
        }

        public String getCliente() {
            return cliente;
        }

        public String getTipoMembresia() {
            return tipoMembresia;
        }

        public String getEstado() {
            return estado;
        }

        public Date getFechaVencimiento() {
            return fechaVencimiento;
        }

        public Long getDiasRestantes() {
            return diasRestantes;
        }
    }

    public static class ReportePagoItem {

        private final Long idPago;
        private final String cliente;
        private final String tipoMembresia;
        private final String metodoPago;
        private final BigDecimal monto;
        private final Date fechaPago;
        private final String activo;

        public ReportePagoItem(Long idPago,
                String cliente,
                String tipoMembresia,
                String metodoPago,
                BigDecimal monto,
                Date fechaPago,
                String activo) {
            this.idPago = idPago;
            this.cliente = cliente;
            this.tipoMembresia = tipoMembresia;
            this.metodoPago = metodoPago;
            this.monto = monto;
            this.fechaPago = fechaPago;
            this.activo = activo;
        }

        public Long getIdPago() {
            return idPago;
        }

        public String getCliente() {
            return cliente;
        }

        public String getTipoMembresia() {
            return tipoMembresia;
        }

        public String getMetodoPago() {
            return metodoPago;
        }

        public BigDecimal getMonto() {
            return monto;
        }

        public Date getFechaPago() {
            return fechaPago;
        }

        public String getActivo() {
            return activo;
        }
    }
}
