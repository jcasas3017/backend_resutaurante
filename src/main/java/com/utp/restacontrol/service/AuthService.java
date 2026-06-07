package com.utp.restacontrol.service;

import com.utp.restacontrol.model.Usuario;
import com.utp.restacontrol.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

        private final UsuarioRepository usuarioRepository;
        private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        public AuthService(UsuarioRepository usuarioRepository) {
                this.usuarioRepository = usuarioRepository;
        }

    public Usuario login(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }

        Usuario user = usuarioRepository
                .findByUsernameIgnoreCaseAndActivoTrue(username.trim())
                .orElse(null);

        if (user == null || !passwordMatches(password, user.getPassword())) {
            return null;
        }

        return user;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findByActivoTrueOrderByUsernameAsc();
    }

        private boolean passwordMatches(String rawPassword, String storedPassword) {
                if (storedPassword == null || storedPassword.isBlank()) {
                        return false;
                }

                if (isBcryptHash(storedPassword)) {
                        return passwordEncoder.matches(rawPassword, storedPassword);
                }

                return rawPassword.equals(storedPassword);
        }

        private boolean isBcryptHash(String value) {
                return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
        }
}