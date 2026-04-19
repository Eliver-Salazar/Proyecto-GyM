package com.gym.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "PARAMETROS_SISTEMA")
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PARAMETRO")
    private Long idParametro;

    @Column(name = "CLAVE", nullable = false, unique = true, length = 50)
    private String clave;

    @Column(name = "VALOR", nullable = false, length = 200)
    private String valor;

    @Column(name = "DESCRIPCION", length = 200)
    private String descripcion;

    public Long getIdParametro() {
        return idParametro;
    }

    public void setIdParametro(Long idParametro) {
        this.idParametro = idParametro;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}