package com.utp.restacontrol.controller;

import com.utp.restacontrol.service.AtencionService;
import com.utp.restacontrol.service.CocinaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final AtencionService atencionService;
    private final CocinaService cocinaService;

    public ApiController(
            AtencionService atencionService,
            CocinaService cocinaService
    ) {
        this.atencionService = atencionService;
        this.cocinaService = cocinaService;
    }

    @GetMapping("/atenciones")
    public Object atenciones() {
        return atencionService.listarAtenciones();
    }

    @GetMapping("/cocina")
    public Object cocina() {
        return cocinaService.listarItemsCocina();
    }
}
