package com.utp.restacontrol.controller;

import com.utp.restacontrol.service.AtencionService;
import com.utp.restacontrol.service.CocinaService;
import com.utp.restacontrol.service.OperacionBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final AtencionService atencionService;
    private final CocinaService cocinaService;

    public ApiController(
            AtencionService atencionService,
            CocinaService cocinaService) {
        this.atencionService = atencionService;
        this.cocinaService = cocinaService;
    }

    @GetMapping("/atenciones")
    public Object atenciones() {
        return atencionService.listarAtenciones();
    }

    @GetMapping("/cocina")
    public Map<String, Object> cocina(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String search) {
        List<Map<String, Object>> items = cocinaService.listarItemsCocina(estado, search);
        return Map.of("items", items);
    }

    @ExceptionHandler(OperacionBusinessException.class)
    public ResponseEntity<?> handleBusiness(OperacionBusinessException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        body.put("code", ex.getCode());
        if (ex.getDetails() != null) {
            body.put("details", ex.getDetails());
        }
        return ResponseEntity.badRequest().body(body);
    }
}
