package com.utp.restacontrol.dto.operacion;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.UUID;

public class OcuparMesaRequest {

    @JsonAlias({"id_cliente", "clienteId", "idClienteSeleccionado"})
    private UUID idCliente;
    @JsonAlias({"id_mozo", "mozoId", "idUsuarioMozo", "id_usuario_mozo", "id_usuario", "idUsuario"})
    private UUID idMozo;
    private String notas;
    @JsonAlias({"detalle", "detalles"})
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
