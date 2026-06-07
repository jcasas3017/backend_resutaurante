package com.utp.restacontrol.model;

public class DetallePedido {

    private String plato;
    private Integer cantidad;
    private Double precioUnitario;

    public DetallePedido(String plato, Integer cantidad, Double precioUnitario) {
        this.plato = plato;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public String getPlato() { return plato; }
    public Integer getCantidad() { return cantidad; }
    public Double getPrecioUnitario() { return precioUnitario; }

    public Double getSubtotal() {
        return cantidad * precioUnitario;
    }
}