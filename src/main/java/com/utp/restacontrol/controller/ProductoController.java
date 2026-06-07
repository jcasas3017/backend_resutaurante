package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.producto.ProductoCreateRequest;
import com.utp.restacontrol.dto.producto.ProductoEstadoRequest;
import com.utp.restacontrol.dto.producto.ProductoUpdateRequest;
import com.utp.restacontrol.service.ProductoCrudService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoCrudService productoCrudService;

    public ProductoController(ProductoCrudService productoCrudService) {
        this.productoCrudService = productoCrudService;
    }

    @GetMapping("/unidades")
    public ResponseEntity<?> unidades() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Unidades obtenidas",
                "data", List.of(
                        Map.of("value", "unidad", "label", "Unidad / Botella / Lata"),
                        Map.of("value", "litro", "label", "Litro"),
                        Map.of("value", "kg", "label", "Kilogramo"),
                        Map.of("value", "gramo", "label", "Gramo")
                )
        ));
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<?> result = productoCrudService.listar(search, activo, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Productos obtenidos",
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
                    "message", "Producto obtenido",
                    "data", productoCrudService.obtener(id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProductoCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Producto creado",
                    "data", productoCrudService.crear(request)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @RequestBody ProductoUpdateRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Producto actualizado",
                    "data", productoCrudService.actualizar(id, request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Producto no encontrado".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id, @RequestBody ProductoEstadoRequest request) {
        try {
            if (request == null || request.getActivo() == null) {
                return ResponseEntity.badRequest().body(error("El campo activo es obligatorio"));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado",
                    "data", productoCrudService.cambiarEstado(id, request.getActivo())
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        try {
            productoCrudService.eliminarLogico(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Producto inactivado",
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
