package com.utp.restacontrol.dto.plato;

import java.util.UUID;

public class PlatoCreateRequest {

    private UUID idCategoria;
    private String nombre;
    private String descripcion;
    private Double precio;
    private Boolean disponible;
    private Boolean activo;

    public UUID getIdCategoria() { return idCategoria; }
    public void setIdCategoria(UUID idCategoria) { this.idCategoria = idCategoria; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
