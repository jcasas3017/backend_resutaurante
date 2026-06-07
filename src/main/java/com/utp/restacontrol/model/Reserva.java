package com.utp.restacontrol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "codigo", insertable = false, updatable = false)
    private String codigo;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(name = "nombre_contacto", nullable = false)
    private String nombreContacto;

    @Column(name = "notas")
    private String observacion;

    @Column(name = "id_mesa", nullable = false)
    private UUID idMesa;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "cantidad_personas", nullable = false)
    private Integer cantidadPersonas;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "confirmada", nullable = false)
    private Boolean confirmada;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    public Reserva() {
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getTipo() { return tipo; }
    public UUID getIdCliente() { return idCliente; }
    public String getNombreContacto() { return nombreContacto; }
    public String getObservacion() { return observacion; }
    public UUID getIdMesa() { return idMesa; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public Integer getCantidadPersonas() { return cantidadPersonas; }
    public String getEstado() { return estado; }
    public Boolean getConfirmada() { return confirmada; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setIdCliente(UUID idCliente) { this.idCliente = idCliente; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public void setIdMesa(UUID idMesa) { this.idMesa = idMesa; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public void setCantidadPersonas(Integer cantidadPersonas) { this.cantidadPersonas = cantidadPersonas; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setConfirmada(Boolean confirmada) { this.confirmada = confirmada; }
}
