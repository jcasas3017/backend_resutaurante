package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.usuario.UsuarioCreateRequest;
import com.utp.restacontrol.dto.usuario.UsuarioEstadoRequest;
import com.utp.restacontrol.dto.usuario.UsuarioPasswordRequest;
import com.utp.restacontrol.dto.usuario.UsuarioUpdateRequest;
import com.utp.restacontrol.service.UsuarioCrudService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioCrudService usuarioCrudService;

    public UsuarioController(UsuarioCrudService usuarioCrudService) {
        this.usuarioCrudService = usuarioCrudService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<?> result = usuarioCrudService.listar(search, rol, activo, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuarios obtenidos",
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
                    "message", "Usuario obtenido",
                    "data", usuarioCrudService.obtener(id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Usuario creado",
                    "data", usuarioCrudService.crear(request)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @RequestBody UsuarioUpdateRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario actualizado",
                    "data", usuarioCrudService.actualizar(id, request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Usuario no encontrado".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id, @RequestBody UsuarioEstadoRequest request) {
        try {
            if (request == null || request.getActivo() == null) {
                return ResponseEntity.badRequest().body(error("El campo activo es obligatorio"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado",
                    "data", usuarioCrudService.cambiarEstado(id, request.getActivo())
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<?> resetPassword(@PathVariable UUID id, @RequestBody UsuarioPasswordRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password actualizada",
                    "data", usuarioCrudService.resetPassword(id, request == null ? null : request.getPassword())
            ));
        } catch (IllegalArgumentException ex) {
            if ("Usuario no encontrado".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Current-Username", required = false) String currentUsername
    ) {
        try {
            usuarioCrudService.eliminarLogico(id, currentUsername);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Usuario inactivado",
                    "data", Map.of("id", id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    private Map<String, Object> error(String message) {
        return Map.of(
                "success", false,
                "message", message
        );
    }
}
