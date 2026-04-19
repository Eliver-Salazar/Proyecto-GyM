package com.gym.domain;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "BITACORA_ACCESO")
public class BitacoraAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ACCESO")
    private Long idAcceso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SUCURSAL", nullable = false)
    private Sucursal sucursal;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "FECHA_HORA", nullable = false)
    private Date fechaHora;

    @Column(name = "RESULTADO", nullable = false, length = 10)
    private String resultado;

    @Column(name = "MOTIVO", length = 120)
    private String motivo;

    public Long getIdAcceso() {
        return idAcceso;
    }

    public void setIdAcceso(Long idAcceso) {
        this.idAcceso = idAcceso;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Sucursal getSucursal() {
        return sucursal;
    }

    public void setSucursal(Sucursal sucursal) {
        this.sucursal = sucursal;
    }

    public Date getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Date fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}