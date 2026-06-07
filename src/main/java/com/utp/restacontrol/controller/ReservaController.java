package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.reserva.ReservaCreateRequest;
import com.utp.restacontrol.dto.reserva.ReservaEstadoRequest;
import com.utp.restacontrol.dto.reserva.ReservaSituacionRequest;
import com.utp.restacontrol.dto.reserva.ReservaUpdateRequest;
import com.utp.restacontrol.model.EstadoReserva;
import com.utp.restacontrol.service.ReservaCrudService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")
public class ReservaController {

    private final ReservaCrudService reservaCrudService;

    public ReservaController(ReservaCrudService reservaCrudService) {
        this.reservaCrudService = reservaCrudService;
    }

    @GetMapping("/estados")
    public ResponseEntity<?> estados() {
        List<Map<String, String>> data = Arrays.stream(EstadoReserva.values())
                .map(value -> Map.of("value", value.name(), "label", pretty(value.name())))
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estados de reserva obtenidos",
                "data", data
        ));
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID idMesa,
            @RequestParam(required = false) UUID idCliente,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<?> result = reservaCrudService.listar(search, estado, idMesa, idCliente, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Reservas obtenidas",
                "data", result.getContent(),
                "total", result.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping(value = "/exportar-excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID idMesa,
            @RequestParam(required = false) UUID idCliente
    ) {
        byte[] archivo = reservaCrudService.exportarExcel(search, estado, idMesa, idCliente);
        String nombreArchivo = "reservas_" + LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header("Content-Disposition", "attachment; filename=" + nombreArchivo)
                .body(archivo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reserva obtenida",
                    "data", reservaCrudService.obtener(id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ReservaCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Reserva creada",
                    "data", reservaCrudService.crear(request)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @RequestBody ReservaUpdateRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reserva actualizada",
                    "data", reservaCrudService.actualizar(id, request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Reserva no encontrada".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id, @RequestBody ReservaEstadoRequest request) {
        try {
            if (request == null || request.getConfirmada() == null) {
                return ResponseEntity.badRequest().body(error("El campo confirmada es obligatorio"));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado",
                    "data", reservaCrudService.cambiarEstado(id, request.getConfirmada())
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/situacion")
    public ResponseEntity<?> cambiarSituacion(@PathVariable UUID id, @RequestBody ReservaSituacionRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Situacion actualizada",
                    "data", reservaCrudService.cambiarSituacion(id, request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Reserva no encontrada".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        try {
            reservaCrudService.eliminarLogico(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Reserva inactivada",
                    "data", Map.of("id", id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    private String pretty(String value) {
        return switch (value) {
            case "pendiente" -> "Pendiente";
            case "confirmada" -> "Confirmada";
            case "cancelada" -> "Cancelada";
            case "atendida" -> "Atendida";
            default -> value;
        };
    }

    private Map<String, Object> error(String message) {
        return Map.of(
                "success", false,
                "message", message
        );
    }
}
