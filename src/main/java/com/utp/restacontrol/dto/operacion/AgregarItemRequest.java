package com.utp.restacontrol.dto.operacion;

import java.util.UUID;

public class AgregarItemRequest {

    private String tipoItem;
    private UUID idItem;
    private Integer cantidad;
    private String observaciones;

    public String getTipoItem() { return tipoItem; }
    public void setTipoItem(String tipoItem) { this.tipoItem = tipoItem; }

    public UUID getIdItem() { return idItem; }
    public void setIdItem(UUID idItem) { this.idItem = idItem; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
