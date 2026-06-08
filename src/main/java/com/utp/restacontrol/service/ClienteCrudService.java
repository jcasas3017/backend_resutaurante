package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.cliente.ClienteCreateRequest;
import com.utp.restacontrol.dto.cliente.ClienteDto;
import com.utp.restacontrol.dto.cliente.ClienteUpdateRequest;
import com.utp.restacontrol.model.Cliente;
import com.utp.restacontrol.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ClienteCrudService {

    private final ClienteRepository clienteRepository;

    public ClienteCrudService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Page<ClienteDto> listar(String search, Boolean activo, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "nombres", "apellidos"));
        return clienteRepository.buscarClientes(search, activo, pageable).map(this::toDto);
    }

    public ClienteDto obtener(UUID id) {
        return toDto(requireCliente(id));
    }

    public ClienteDto buscarPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El documento es obligatorio");
        }
        String value = documento.trim();
        Cliente cliente = clienteRepository.findByDocumento(value)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        return toDto(cliente);
    }

    public ClienteDto crear(ClienteCreateRequest request) {
        validarCrear(request);

        Cliente cliente = new Cliente();
        cliente.setNombres(request.getNombres().trim());
        cliente.setApellidos(request.getApellidos().trim());
        cliente.setDocumento(request.getDocumento().trim());
        cliente.setTelefono(trimToNull(request.getTelefono()));
        cliente.setEmail(trimToNull(request.getEmail()));
        cliente.setActivo(request.getActivo() == null || request.getActivo());

        Cliente saved = clienteRepository.save(cliente);
        return toDto(requireCliente(saved.getId()));
    }

    public ClienteDto actualizar(UUID id, ClienteUpdateRequest request) {
        validarActualizar(id, request);

        Cliente cliente = requireCliente(id);
        cliente.setNombres(request.getNombres().trim());
        cliente.setApellidos(request.getApellidos().trim());
        cliente.setDocumento(request.getDocumento().trim());
        cliente.setTelefono(trimToNull(request.getTelefono()));
        cliente.setEmail(trimToNull(request.getEmail()));
        cliente.setActivo(request.getActivo() == null || request.getActivo());

        Cliente saved = clienteRepository.save(cliente);
        return toDto(requireCliente(saved.getId()));
    }

    public ClienteDto cambiarEstado(UUID id, boolean activo) {
        Cliente cliente = requireCliente(id);
        cliente.setActivo(activo);
        Cliente saved = clienteRepository.save(cliente);
        return toDto(requireCliente(saved.getId()));
    }

    public void eliminarLogico(UUID id) {
        Cliente cliente = requireCliente(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private Cliente requireCliente(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    private void validarCrear(ClienteCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombres(), request.getApellidos(), request.getDocumento(), request.getEmail());

        String documento = request.getDocumento().trim();
        if (clienteRepository.existsByDocumento(documento)) {
            throw new IllegalStateException("El documento del cliente ya existe");
        }

        String email = trimToNull(request.getEmail());
        if (email != null && clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("El email del cliente ya existe");
        }
    }

    private void validarActualizar(UUID id, ClienteUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombres(), request.getApellidos(), request.getDocumento(), request.getEmail());

        String documento = request.getDocumento().trim();
        if (clienteRepository.existsByDocumentoAndIdNot(documento, id)) {
            throw new IllegalStateException("El documento del cliente ya existe");
        }

        String email = trimToNull(request.getEmail());
        if (email != null && clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new IllegalStateException("El email del cliente ya existe");
        }
    }

    private void validarCamposBase(String nombres, String apellidos, String documento, String email) {
        if (nombres == null || nombres.isBlank()) {
            throw new IllegalArgumentException("Los nombres son obligatorios");
        }
        if (apellidos == null || apellidos.isBlank()) {
            throw new IllegalArgumentException("Los apellidos son obligatorios");
        }
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El documento es obligatorio");
        }
        if (documento.trim().length() < 8) {
            throw new IllegalArgumentException("El documento debe tener al menos 8 caracteres");
        }
        String emailValue = trimToNull(email);
        if (emailValue != null && !emailValue.contains("@")) {
            throw new IllegalArgumentException("El email no es válido");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ClienteDto toDto(Cliente cliente) {
        ClienteDto dto = new ClienteDto();
        dto.setId(cliente.getId());
        dto.setCodigo(cliente.getCodigo());
        dto.setNombres(cliente.getNombres());
        dto.setApellidos(cliente.getApellidos());
        dto.setDocumento(cliente.getDocumento());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setActivo(cliente.getActivo());
        dto.setFechaCreacion(cliente.getFechaCreacion());
        dto.setFechaActualizacion(cliente.getFechaActualizacion());
        return dto;
    }
}
