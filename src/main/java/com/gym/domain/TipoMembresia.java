package com.gym.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "TIPOS_MEMBRESIA")
public class TipoMembresia implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TIPO_MEMBRESIA")
    private Long idTipoMembresia;

    @Column(name = "NOMBRE", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "CANTIDAD_DIAS", nullable = false)
    private Integer cantidadDias;

    @Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "ACCESO_GLOBAL", nullable = false, length = 1)
    private String accesoGlobal = "N";

    public TipoMembresia() {
    }

    public Long getIdTipoMembresia() {
        return idTipoMembresia;
    }

    public void setIdTipoMembresia(Long idTipoMembresia) {
        this.idTipoMembresia = idTipoMembresia;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getCantidadDias() {
        return cantidadDias;
    }

    public void setCantidadDias(Integer cantidadDias) {
        this.cantidadDias = cantidadDias;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getAccesoGlobal() {
        return accesoGlobal;
    }

    public void setAccesoGlobal(String accesoGlobal) {
        this.accesoGlobal = accesoGlobal;
    }
}