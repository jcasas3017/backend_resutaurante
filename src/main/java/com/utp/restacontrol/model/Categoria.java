package com.utp.restacontrol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "codigo", insertable = false, updatable = false)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    public Categoria() {
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public Integer getOrden() { return orden; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}