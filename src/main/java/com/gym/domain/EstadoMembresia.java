package com.gym.domain;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "ESTADOS_MEMBRESIAS")
public class EstadoMembresia implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ESTADO_MEMBRESIA")
    private Long idEstadoMembresia;
    
    @Column(name = "NOMBRE_ESTADO", nullable = false, length = 30, unique = true)
    private String nombreEstado;

    public EstadoMembresia() {
    }

    public Long getIdEstadoMembresia() {
        return idEstadoMembresia;
    }

    public void setIdEstadoMembresia(Long idEstadoMembresia) {
        this.idEstadoMembresia = idEstadoMembresia;
    }

    public String getNombreEstado() {
        return nombreEstado;
    }

    public void setNombreEstado(String nombreEstado) {
        this.nombreEstado = nombreEstado;
    }
}