package com.utp.restacontrol.model;

public class CocinaItem {

    private String pedido;
    private String mesa;
    private String plato;
    private Integer cantidad;
    private String horaEnvio;
    private String estado;

    public CocinaItem(String pedido, String mesa, String plato, Integer cantidad, String horaEnvio, String estado) {
        this.pedido = pedido;
        this.mesa = mesa;
        this.plato = plato;
        this.cantidad = cantidad;
        this.horaEnvio = horaEnvio;
        this.estado = estado;
    }

    public String getPedido() { return pedido; }
    public String getMesa() { return mesa; }
    public String getPlato() { return plato; }
    public Integer getCantidad() { return cantidad; }
    public String getHoraEnvio() { return horaEnvio; }
    public String getEstado() { return estado; }
}