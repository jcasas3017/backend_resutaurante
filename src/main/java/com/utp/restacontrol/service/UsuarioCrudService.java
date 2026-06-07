package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.usuario.UsuarioCreateRequest;
import com.utp.restacontrol.dto.usuario.UsuarioDto;
import com.utp.restacontrol.dto.usuario.UsuarioUpdateRequest;
import com.utp.restacontrol.model.Usuario;
import com.utp.restacontrol.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class UsuarioCrudService {

    private static final Set<String> ROLES_VALIDOS = Set.of(
            "Administrador", "Recepcion", "Mozo", "Cajero", "Cocinero"
    );

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioCrudService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Page<UsuarioDto> listar(String search, String rol, Boolean activo, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        return usuarioRepository.buscarUsuarios(search, rol, activo, pageable).map(this::toDto);
    }

    public UsuarioDto obtener(UUID id) {
        return toDto(requireUsuario(id));
    }

    public UsuarioDto crear(UsuarioCreateRequest request) {
        validarCrear(request);

        Usuario user = new Usuario();
        user.setNombres(request.getNombres().trim());
        user.setApellidos(request.getApellidos().trim());
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRol(request.getRol().trim());
        user.setActivo(request.getActivo() == null || request.getActivo());

        Usuario saved = usuarioRepository.save(user);
        return toDto(requireUsuario(saved.getId()));
    }

    public UsuarioDto actualizar(UUID id, UsuarioUpdateRequest request) {
        validarActualizar(id, request);

        Usuario user = requireUsuario(id);
        user.setNombres(request.getNombres().trim());
        user.setApellidos(request.getApellidos().trim());
        user.setUsername(request.getUsername().trim());
        user.setRol(request.getRol().trim());
        user.setActivo(request.getActivo() == null || request.getActivo());

        Usuario saved = usuarioRepository.save(user);
        return toDto(requireUsuario(saved.getId()));
    }

    public UsuarioDto cambiarEstado(UUID id, boolean activo) {
        Usuario user = requireUsuario(id);
        user.setActivo(activo);
        Usuario saved = usuarioRepository.save(user);
        return toDto(requireUsuario(saved.getId()));
    }

    public UsuarioDto resetPassword(UUID id, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La password es obligatoria");
        }

        Usuario user = requireUsuario(id);
        user.setPassword(passwordEncoder.encode(password));
        Usuario saved = usuarioRepository.save(user);
        return toDto(requireUsuario(saved.getId()));
    }

    public void eliminarLogico(UUID id, String currentUsername) {
        Usuario user = requireUsuario(id);

        if (currentUsername != null && !currentUsername.isBlank()
                && currentUsername.equalsIgnoreCase(user.getUsername())) {
            throw new IllegalStateException("No puedes eliminar el usuario logueado");
        }

        user.setActivo(false);
        usuarioRepository.save(user);
    }

    private Usuario requireUsuario(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void validarCrear(UsuarioCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombres(), request.getApellidos(), request.getUsername(), request.getRol());
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("La password es obligatoria");
        }
        if (usuarioRepository.existsByUsernameIgnoreCase(request.getUsername().trim())) {
            throw new IllegalStateException("El username ya existe");
        }
    }

    private void validarActualizar(UUID id, UsuarioUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombres(), request.getApellidos(), request.getUsername(), request.getRol());
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(request.getUsername().trim(), id)) {
            throw new IllegalStateException("El username ya existe");
        }
    }

    private void validarCamposBase(String nombres, String apellidos, String username, String rol) {
        if (nombres == null || nombres.isBlank()) {
            throw new IllegalArgumentException("Los nombres son obligatorios");
        }
        if (apellidos == null || apellidos.isBlank()) {
            throw new IllegalArgumentException("Los apellidos son obligatorios");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }
        if (rol == null || rol.isBlank() || !ROLES_VALIDOS.contains(rol.trim())) {
            throw new IllegalArgumentException("Rol no válido");
        }
    }

    private UsuarioDto toDto(Usuario user) {
        UsuarioDto dto = new UsuarioDto();
        dto.setId(user.getId());
        dto.setCodigo(user.getCodigo());
        dto.setNombres(user.getNombres());
        dto.setApellidos(user.getApellidos());
        dto.setUsername(user.getUsername());
        dto.setRol(user.getRol());
        dto.setActivo(user.getActivo());
        dto.setFechaCreacion(user.getFechaCreacion());
        dto.setFechaActualizacion(user.getFechaActualizacion());
        return dto;
    }
}
