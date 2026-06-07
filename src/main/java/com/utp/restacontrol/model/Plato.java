package com.utp.restacontrol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "platos")
public class Plato {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "codigo", insertable = false, updatable = false)
    private String codigo;

    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoriaRef;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "disponible", nullable = false)
    private Boolean disponible;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    public Plato() {
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public UUID getIdCategoria() { return categoriaRef != null ? categoriaRef.getId() : null; }
    public String getCategoria() { return categoriaRef != null ? categoriaRef.getNombre() : null; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Double getPrecio() { return precio; }
    public Boolean getDisponible() { return disponible; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    public void setCategoriaRef(Categoria categoriaRef) { this.categoriaRef = categoriaRef; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecio(Double precio) { this.precio = precio; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}