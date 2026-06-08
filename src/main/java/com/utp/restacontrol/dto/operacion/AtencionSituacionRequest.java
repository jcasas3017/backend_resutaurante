package com.utp.restacontrol.dto.operacion;

public class AtencionSituacionRequest {

    private String estado;
    private String motivo;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}