package com.utp.restacontrol.dto.operacion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobrarAtencionRequestTest {

    @Test
    void shouldExposePaymentPayloadFields() {
        CobrarAtencionRequest request = new CobrarAtencionRequest();
        request.setMetodoPago("Efectivo");
        request.setSubtotal(61.50);
        request.setPropina(0.00);
        request.setTotal(61.50);
        request.setObservaciones("");
        request.setGenerarComprobante(true);

        assertEquals("Efectivo", request.getMetodoPago());
        assertEquals(61.50, request.getSubtotal());
        assertEquals(0.00, request.getPropina());
        assertEquals(61.50, request.getTotal());
        assertEquals("", request.getObservaciones());
        assertTrue(request.getGenerarComprobante());
    }
}
