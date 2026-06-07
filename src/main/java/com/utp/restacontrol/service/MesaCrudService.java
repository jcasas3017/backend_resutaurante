package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.mesa.MesaCreateRequest;
import com.utp.restacontrol.dto.mesa.MesaDto;
import com.utp.restacontrol.dto.mesa.MesaUpdateRequest;
import com.utp.restacontrol.model.EstadoMesa;
import com.utp.restacontrol.model.Mesa;
import com.utp.restacontrol.repository.MesaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

@Service
public class MesaCrudService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^(.*?)(\\d+)$");

    private final MesaRepository mesaRepository;

    public MesaCrudService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    public Page<MesaDto> listar(String search, Boolean activo, String estado, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "codigo"));

        String estadoFiltro = estado == null || estado.isBlank() ? null : normalizeEstado(estado);
        return mesaRepository.buscarMesas(search, activo, estadoFiltro, pageable).map(this::toDto);
    }

    public MesaDto obtener(UUID id) {
        return toDto(requireMesa(id));
    }

    public MesaDto crear(MesaCreateRequest request) {
        validarCrear(request);

        Mesa mesa = new Mesa();
        mesa.setCodigo(generarCodigoSiguiente());
        mesa.setCapacidad(request.getCapacidad());
        mesa.setUbicacion(trimToNull(request.getUbicacion()));
        mesa.setEstado(resolveEstado(request.getEstado()));
        mesa.setActivo(request.getActivo() == null || request.getActivo());

        Mesa saved = mesaRepository.save(mesa);
        return toDto(requireMesa(saved.getId()));
    }

    public MesaDto actualizar(UUID id, MesaUpdateRequest request) {
        validarActualizar(id, request);

        Mesa mesa = requireMesa(id);
        mesa.setCapacidad(request.getCapacidad());
        mesa.setUbicacion(trimToNull(request.getUbicacion()));
        mesa.setEstado(resolveEstado(request.getEstado()));
        mesa.setActivo(request.getActivo() == null || request.getActivo());

        Mesa saved = mesaRepository.save(mesa);
        return toDto(requireMesa(saved.getId()));
    }

    public MesaDto cambiarEstado(UUID id, boolean activo) {
        Mesa mesa = requireMesa(id);
        mesa.setActivo(activo);
        Mesa saved = mesaRepository.save(mesa);
        return toDto(requireMesa(saved.getId()));
    }

    public MesaDto cambiarSituacion(UUID id, String estado) {
        Mesa mesa = requireMesa(id);
        mesa.setEstado(resolveEstado(estado));
        Mesa saved = mesaRepository.save(mesa);
        return toDto(requireMesa(saved.getId()));
    }

    public void eliminarLogico(UUID id) {
        Mesa mesa = requireMesa(id);
        mesa.setActivo(false);
        mesaRepository.save(mesa);
    }

    public String siguienteCodigo() {
        return generarCodigoSiguiente();
    }

    private Mesa requireMesa(UUID id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
    }

    private void validarCrear(MesaCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getCapacidad(), request.getEstado());
    }

    private void validarActualizar(UUID id, MesaUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getCapacidad(), request.getEstado());
    }

    private void validarCamposBase(Integer capacidad, String estado) {
        if (capacidad == null || capacidad < 1) {
            throw new IllegalArgumentException("La capacidad debe ser mayor a 0");
        }
        resolveEstado(estado);
    }

    private String generarCodigoSiguiente() {
        Mesa ultimaMesa = mesaRepository.findTopByOrderByCodigoDesc();

        if (ultimaMesa == null || ultimaMesa.getCodigo() == null || ultimaMesa.getCodigo().isBlank()) {
            return "M-001";
        }

        Matcher matcher = CODE_PATTERN.matcher(ultimaMesa.getCodigo().trim());
        if (!matcher.matches()) {
            return "M-001";
        }

        String prefijo = matcher.group(1);
        int secuencia = Integer.parseInt(matcher.group(2)) + 1;
        int ancho = matcher.group(2).length();
        return prefijo + String.format("%0" + ancho + "d", secuencia);
    }

    private String resolveEstado(String estado) {
        String resolved = estado == null || estado.isBlank() ? EstadoMesa.disponible.name() : normalizeEstado(estado);

        for (EstadoMesa value : EstadoMesa.values()) {
            if (value.name().equalsIgnoreCase(resolved)) {
                return value.name();
            }
        }
        throw new IllegalArgumentException("Estado de mesa no valido. Usa: disponible, ocupada, reservada, mantenimiento");
    }

    private String normalizeEstado(String estado) {
        return estado.trim().toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MesaDto toDto(Mesa mesa) {
        MesaDto dto = new MesaDto();
        dto.setId(mesa.getId());
        dto.setCodigo(mesa.getCodigo());
        dto.setCapacidad(mesa.getCapacidad());
        dto.setUbicacion(mesa.getUbicacion());
        dto.setEstado(mesa.getEstado());
        dto.setActivo(mesa.getActivo());
        dto.setFechaCreacion(mesa.getFechaCreacion());
        dto.setFechaActualizacion(mesa.getFechaActualizacion());
        return dto;
    }
}
