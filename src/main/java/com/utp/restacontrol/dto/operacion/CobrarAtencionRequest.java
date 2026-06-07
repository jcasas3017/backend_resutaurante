package com.utp.restacontrol.dto.operacion;

public class CobrarAtencionRequest {

    private String metodoPago;
    private Double propina;
    private String observaciones;
    private Boolean generarComprobante;

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public Double getPropina() { return propina; }
    public void setPropina(Double propina) { this.propina = propina; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Boolean getGenerarComprobante() { return generarComprobante; }
    public void setGenerarComprobante(Boolean generarComprobante) { this.generarComprobante = generarComprobante; }
}
