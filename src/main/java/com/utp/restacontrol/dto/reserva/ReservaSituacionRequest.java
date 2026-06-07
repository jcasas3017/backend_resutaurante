package com.utp.restacontrol.dto.reserva;

public class ReservaSituacionRequest {

    private String estado;
    private Boolean confirmada;

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Boolean getConfirmada() { return confirmada; }
    public void setConfirmada(Boolean confirmada) { this.confirmada = confirmada; }
}
