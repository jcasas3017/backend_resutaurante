package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.mesa.MesaCreateRequest;
import com.utp.restacontrol.dto.mesa.MesaEstadoRequest;
import com.utp.restacontrol.dto.mesa.MesaSituacionRequest;
import com.utp.restacontrol.dto.mesa.MesaUpdateRequest;
import com.utp.restacontrol.model.EstadoMesa;
import com.utp.restacontrol.service.MesaCrudService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mesas")
@CrossOrigin(origins = "*")
public class MesaController {

    private final MesaCrudService mesaCrudService;

    public MesaController(MesaCrudService mesaCrudService) {
        this.mesaCrudService = mesaCrudService;
    }

    @GetMapping("/next-codigo")
    public ResponseEntity<?> siguienteCodigo() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Codigo de mesa generado",
                "data", Map.of("codigo", mesaCrudService.siguienteCodigo())
        ));
    }

    @GetMapping("/estados")
    public ResponseEntity<?> estados() {
        List<Map<String, String>> data = Arrays.stream(EstadoMesa.values())
                .map(value -> Map.of("value", value.name(), "label", pretty(value.name())))
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Estados de mesa obtenidos",
                "data", data
        ));
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<?> result = mesaCrudService.listar(search, activo, estado, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mesas obtenidas",
                "data", result.getContent(),
                "total", result.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mesa obtenida",
                    "data", mesaCrudService.obtener(id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody MesaCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Mesa creada",
                    "data", mesaCrudService.crear(request)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @RequestBody MesaUpdateRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mesa actualizada",
                    "data", mesaCrudService.actualizar(id, request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Mesa no encontrada".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id, @RequestBody MesaEstadoRequest request) {
        try {
            if (request == null || request.getActivo() == null) {
                return ResponseEntity.badRequest().body(error("El campo activo es obligatorio"));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado",
                    "data", mesaCrudService.cambiarEstado(id, request.getActivo())
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/situacion")
    public ResponseEntity<?> cambiarSituacion(@PathVariable UUID id, @RequestBody MesaSituacionRequest request) {
        try {
            if (request == null || request.getEstado() == null || request.getEstado().isBlank()) {
                return ResponseEntity.badRequest().body(error("El campo estado es obligatorio"));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Situacion actualizada",
                    "data", mesaCrudService.cambiarSituacion(id, request.getEstado())
            ));
        } catch (IllegalArgumentException ex) {
            if ("Mesa no encontrada".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        try {
            mesaCrudService.eliminarLogico(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mesa inactivada",
                    "data", Map.of("id", id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    private String pretty(String value) {
        return switch (value) {
            case "disponible" -> "Disponible";
            case "ocupada" -> "Ocupada";
            case "reservada" -> "Reservada";
            case "mantenimiento" -> "Mantenimiento";
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
