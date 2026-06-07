package com.utp.restacontrol.model;

import java.util.List;

public class Atencion {

    private String id;
    private String cliente;
    private String mesa;
    private String mozo;
    private String estado;
    private String estadoPago;
    private List<DetallePedido> detalles;

    public Atencion(String id, String cliente, String mesa, String mozo, String estado, String estadoPago, List<DetallePedido> detalles) {
        this.id = id;
        this.cliente = cliente;
        this.mesa = mesa;
        this.mozo = mozo;
        this.estado = estado;
        this.estadoPago = estadoPago;
        this.detalles = detalles;
    }

    public String getId() { return id; }
    public String getCliente() { return cliente; }
    public String getMesa() { return mesa; }
    public String getMozo() { return mozo; }
    public String getEstado() { return estado; }
    public String getEstadoPago() { return estadoPago; }
    public List<DetallePedido> getDetalles() { return detalles; }

    public Double getTotal() {
        return detalles.stream()
                .mapToDouble(DetallePedido::getSubtotal)
                .sum();
    }
}