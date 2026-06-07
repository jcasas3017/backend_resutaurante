package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.operacion.AgregarItemRequest;
import com.utp.restacontrol.dto.operacion.CambiarEstadoItemRequest;
import com.utp.restacontrol.dto.operacion.CobrarAtencionRequest;
import com.utp.restacontrol.dto.operacion.OcuparMesaRequest;
import com.utp.restacontrol.service.ListaMesasOperacionService;
import com.utp.restacontrol.service.OperacionBusinessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ListaMesasController {

    private final ListaMesasOperacionService service;

    public ListaMesasController(ListaMesasOperacionService service) {
        this.service = service;
    }

    @GetMapping("/lista-mesas")
    public ResponseEntity<?> tablero(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora
    ) {
        var data = service.tablero(fechaHora);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OK",
                "data", data,
                "total", data.size(),
                "page", 1,
                "size", data.size()
        ));
    }

    @GetMapping("/lista-mesas/{idMesa}/contexto")
    public ResponseEntity<?> contexto(
            @PathVariable UUID idMesa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora
    ) {
        var data = service.contexto(idMesa, fechaHora);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OK",
                "data", data
        ));
    }

    @PostMapping("/lista-mesas/{idMesa}/ocupar")
    public ResponseEntity<?> ocupar(
            @PathVariable UUID idMesa,
            @RequestBody OcuparMesaRequest request
    ) {
        var data = service.ocuparMesa(idMesa, null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Mesa ocupada",
                "data", data
        ));
    }

    @PostMapping("/lista-mesas/{idMesa}/reservas/{idReserva}/ocupar")
    public ResponseEntity<?> ocuparDesdeReserva(
            @PathVariable UUID idMesa,
            @PathVariable UUID idReserva,
            @RequestBody OcuparMesaRequest request
    ) {
        var data = service.ocuparMesa(idMesa, idReserva, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Mesa ocupada desde reserva",
                "data", data
        ));
    }

    @PostMapping("/atenciones/{idAtencion}/items")
    public ResponseEntity<?> agregarItem(
            @PathVariable UUID idAtencion,
            @RequestBody AgregarItemRequest request
    ) {
        var data = service.agregarItem(idAtencion, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Item agregado",
                "data", data
        ));
    }

    @PatchMapping("/detalle-pedidos/{idDetalle}/estado")
    public ResponseEntity<?> cambiarEstadoItem(
            @PathVariable UUID idDetalle,
            @RequestBody CambiarEstadoItemRequest request
    ) {
        var data = service.cambiarEstadoDetalle(idDetalle, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estado actualizado",
                "data", data
        ));
    }

    @PostMapping("/atenciones/{idAtencion}/cobrar")
    public ResponseEntity<?> cobrar(
            @PathVariable UUID idAtencion,
            @RequestBody CobrarAtencionRequest request
    ) {
        var data = service.cobrarAtencion(idAtencion, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cobro registrado",
                "data", data
        ));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(OperacionBusinessException.class)
    public ResponseEntity<?> handleBusiness(OperacionBusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage(),
                "code", ex.getCode(),
                "details", ex.getDetails()
        ));
    }
}
