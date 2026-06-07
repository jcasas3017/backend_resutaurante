package com.utp.restacontrol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mesas")
public class Mesa {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "activa", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    public Mesa() {
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public Integer getCapacidad() { return capacidad; }
    public String getUbicacion() { return ubicacion; }
    public String getEstado() { return estado; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
