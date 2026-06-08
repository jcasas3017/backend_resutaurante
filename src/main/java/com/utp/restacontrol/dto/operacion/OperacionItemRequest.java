package com.utp.restacontrol.dto.operacion;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.UUID;

public class OperacionItemRequest {

    @JsonAlias({"tipo_item", "tipo"})
    private String tipoItem;
    @JsonAlias({"id_item"})
    private UUID idItem;
    @JsonAlias({"idPlato", "id_plato"})
    private UUID idPlato;
    @JsonAlias({"idProducto", "id_producto"})
    private UUID idProducto;
    private Integer cantidad;
    @JsonAlias({"nota", "notaItem", "observacion"})
    private String observaciones;

    public String getTipoItem() {
        if (tipoItem != null && !tipoItem.isBlank()) {
            return tipoItem;
        }
        if (idPlato != null) {
            return "plato";
        }
        if (idProducto != null) {
            return "producto";
        }
        return tipoItem;
    }
    public void setTipoItem(String tipoItem) { this.tipoItem = tipoItem; }

    public UUID getIdItem() {
        if (idItem != null) {
            return idItem;
        }
        return idPlato != null ? idPlato : idProducto;
    }
    public void setIdItem(UUID idItem) { this.idItem = idItem; }

    public UUID getIdPlato() { return idPlato; }
    public void setIdPlato(UUID idPlato) { this.idPlato = idPlato; }

    public UUID getIdProducto() { return idProducto; }
    public void setIdProducto(UUID idProducto) { this.idProducto = idProducto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
