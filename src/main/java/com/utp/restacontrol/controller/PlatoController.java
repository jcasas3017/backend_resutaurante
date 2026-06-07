package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.plato.PlatoCreateRequest;
import com.utp.restacontrol.dto.plato.PlatoDisponibilidadRequest;
import com.utp.restacontrol.dto.plato.PlatoEstadoRequest;
import com.utp.restacontrol.dto.plato.PlatoUpdateRequest;
import com.utp.restacontrol.service.PlatoCrudService;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/platos")
@CrossOrigin(origins = "*")
public class PlatoController {

    private final PlatoCrudService platoCrudService;

    public PlatoController(PlatoCrudService platoCrudService) {
        this.platoCrudService = platoCrudService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<?> result = platoCrudService.listar(search, categoriaId, activo, disponible, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Platos obtenidos",
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
                    "message", "Plato obtenido",
                    "data", platoCrudService.obtener(id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody PlatoCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Plato creado",
                    "data", platoCrudService.crear(request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Categoría no encontrada".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @RequestBody PlatoUpdateRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Plato actualizado",
                    "data", platoCrudService.actualizar(id, request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Plato no encontrado".equals(ex.getMessage()) || "Categoría no encontrada".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id, @RequestBody PlatoEstadoRequest request) {
        try {
            if (request == null || request.getActivo() == null) {
                return ResponseEntity.badRequest().body(error("El campo activo es obligatorio"));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado",
                    "data", platoCrudService.cambiarEstado(id, request.getActivo())
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<?> cambiarDisponibilidad(@PathVariable UUID id, @RequestBody PlatoDisponibilidadRequest request) {
        try {
            if (request == null || request.getDisponible() == null) {
                return ResponseEntity.badRequest().body(error("El campo disponible es obligatorio"));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Disponibilidad actualizada",
                    "data", platoCrudService.cambiarDisponibilidad(id, request.getDisponible())
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        try {
            platoCrudService.eliminarLogico(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Plato inactivado",
                    "data", Map.of("id", id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    private Map<String, Object> error(String message) {
        return Map.of(
                "success", false,
                "message", message
        );
    }
}
