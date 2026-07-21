package com.utp.restacontrol.controller;

import com.utp.restacontrol.service.AtencionService;
import com.utp.restacontrol.service.CocinaService;
import com.utp.restacontrol.service.ListaMesasOperacionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiControllerTest {

    @Test
    void reporteVentasDebeResponderConFormatoEsperado() throws Exception {
        AtencionService atencionService = mock(AtencionService.class);
        CocinaService cocinaService = mock(CocinaService.class);
        ListaMesasOperacionService listaMesasOperacionService = mock(ListaMesasOperacionService.class);

        when(listaMesasOperacionService.obtenerReporteVentas("7d")).thenReturn(Map.of(
                "success", true,
                "data", Map.of(
                        "ventasDia", 245.5,
                        "ticketPromedio", 61.38,
                        "ocupacionMedia", 67,
                        "mejorDia", Map.of("label", "Lun", "ventas", 320, "ocupacion", 80),
                        "history", java.util.List.of(Map.of("label", "Lun", "ventas", 120, "ocupacion", 60, "atenciones", 2))
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ApiController(atencionService, cocinaService, listaMesasOperacionService)
        ).build();

        mockMvc.perform(get("/api/reportes/ventas").param("periodo", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ventasDia").value(245.5))
                .andExpect(jsonPath("$.data.history[0].label").value("Lun"));
    }

    @Test
    void reporteReservasDebeResponderConFormatoEsperado() throws Exception {
        AtencionService atencionService = mock(AtencionService.class);
        CocinaService cocinaService = mock(CocinaService.class);
        ListaMesasOperacionService listaMesasOperacionService = mock(ListaMesasOperacionService.class);

        when(listaMesasOperacionService.obtenerReporteReservas("7d")).thenReturn(Map.of(
                "success", true,
                "data", Map.of(
                        "totales", Map.of(
                                "totalReservas", 45,
                                "confirmadas", 28,
                                "pendientes", 12,
                                "canceladas", 5,
                                "atencionesEnCurso", 3,
                                "avgTiempoMin", 48),
                        "history", java.util.List.of(Map.of("label", "Lun 20", "confirmadas", 4, "pendientes", 1, "canceladas", 0, "atenciones", 3))
                )
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ApiController(atencionService, cocinaService, listaMesasOperacionService)
        ).build();

        mockMvc.perform(get("/api/reportes/reservas").param("periodo", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totales.totalReservas").value(45))
                .andExpect(jsonPath("$.data.history[0].label").value("Lun 20"));
    }
}
