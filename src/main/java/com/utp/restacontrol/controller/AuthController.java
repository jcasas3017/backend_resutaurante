package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.auth.LoginRequest;
import com.utp.restacontrol.dto.auth.UsuarioResponse;
import com.utp.restacontrol.model.Usuario;
import com.utp.restacontrol.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {

        if (request == null || request.getUsername() == null || request.getPassword() == null
                || request.getUsername().isBlank() || request.getPassword().isBlank()) {
            return Map.of(
                    "success", false,
                    "message", "Debes enviar usuario y contraseña"
            );
        }

        Usuario user = authService.login(request.getUsername(), request.getPassword());

        if (user == null) {
            return Map.of(
                    "success", false,
                    "message", "Usuario o contraseña incorrectos"
            );
        }

        return Map.of(
                "success", true,
                "name", user.getName(),
                "role", user.getRole()
        );
    }

    @GetMapping("/usuarios")
    public Object listarUsuarios() {
        return authService.listarUsuarios()
                .stream()
                .map(u -> new UsuarioResponse(u.getUsername(), u.getName(), u.getRole()))
                .toList();
    }
}