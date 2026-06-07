package com.utp.restacontrol.dto.operacion;

import java.util.List;
import java.util.UUID;

public class OcuparMesaRequest {

    private UUID idCliente;
    private UUID idMozo;
    private String notas;
    private List<OperacionItemRequest> items;

    public UUID getIdCliente() { return idCliente; }
    public void setIdCliente(UUID idCliente) { this.idCliente = idCliente; }

    public UUID getIdMozo() { return idMozo; }
    public void setIdMozo(UUID idMozo) { this.idMozo = idMozo; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public List<OperacionItemRequest> getItems() { return items; }
    public void setItems(List<OperacionItemRequest> items) { this.items = items; }
}
