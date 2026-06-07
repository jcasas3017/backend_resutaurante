package com.utp.restacontrol.dto.reserva;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReservaUpdateRequest {

    private String tipo;
    private UUID idCliente;
    private String nombreContacto;
    private UUID idMesa;
    private LocalDateTime fechaHora;
    private Integer cantidadPersonas;
    private String estado;
    private String observacion;
    private Boolean confirmada;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public UUID getIdCliente() { return idCliente; }
    public void setIdCliente(UUID idCliente) { this.idCliente = idCliente; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public UUID getIdMesa() { return idMesa; }
    public void setIdMesa(UUID idMesa) { this.idMesa = idMesa; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public Integer getCantidadPersonas() { return cantidadPersonas; }
    public void setCantidadPersonas(Integer cantidadPersonas) { this.cantidadPersonas = cantidadPersonas; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Boolean getConfirmada() { return confirmada; }
    public void setConfirmada(Boolean confirmada) { this.confirmada = confirmada; }
}
