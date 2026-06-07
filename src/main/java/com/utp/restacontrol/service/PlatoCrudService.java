package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.plato.PlatoCreateRequest;
import com.utp.restacontrol.dto.plato.PlatoDto;
import com.utp.restacontrol.dto.plato.PlatoUpdateRequest;
import com.utp.restacontrol.model.Categoria;
import com.utp.restacontrol.model.Plato;
import com.utp.restacontrol.repository.CategoriaRepository;
import com.utp.restacontrol.repository.PlatoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlatoCrudService {

    private final PlatoRepository platoRepository;
    private final CategoriaRepository categoriaRepository;

    public PlatoCrudService(PlatoRepository platoRepository, CategoriaRepository categoriaRepository) {
        this.platoRepository = platoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public Page<PlatoDto> listar(String search, UUID categoriaId, Boolean activo, Boolean disponible, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "nombre"));
        return platoRepository.buscarPlatos(search, categoriaId, activo, disponible, pageable).map(this::toDto);
    }

    public PlatoDto obtener(UUID id) {
        return toDto(requirePlato(id));
    }

    public PlatoDto crear(PlatoCreateRequest request) {
        validarCrear(request);

        Categoria categoria = requireCategoria(request.getIdCategoria());

        Plato plato = new Plato();
        plato.setCategoriaRef(categoria);
        plato.setNombre(request.getNombre().trim());
        plato.setDescripcion(request.getDescripcion());
        plato.setPrecio(request.getPrecio());
        plato.setDisponible(request.getDisponible() == null || request.getDisponible());
        plato.setActivo(request.getActivo() == null || request.getActivo());

        Plato saved = platoRepository.save(plato);
        return toDto(requirePlato(saved.getId()));
    }

    public PlatoDto actualizar(UUID id, PlatoUpdateRequest request) {
        validarActualizar(id, request);

        Categoria categoria = requireCategoria(request.getIdCategoria());
        Plato plato = requirePlato(id);

        plato.setCategoriaRef(categoria);
        plato.setNombre(request.getNombre().trim());
        plato.setDescripcion(request.getDescripcion());
        plato.setPrecio(request.getPrecio());
        plato.setDisponible(request.getDisponible() == null || request.getDisponible());
        plato.setActivo(request.getActivo() == null || request.getActivo());

        Plato saved = platoRepository.save(plato);
        return toDto(requirePlato(saved.getId()));
    }

    public PlatoDto cambiarEstado(UUID id, boolean activo) {
        Plato plato = requirePlato(id);
        plato.setActivo(activo);
        Plato saved = platoRepository.save(plato);
        return toDto(requirePlato(saved.getId()));
    }

    public PlatoDto cambiarDisponibilidad(UUID id, boolean disponible) {
        Plato plato = requirePlato(id);
        plato.setDisponible(disponible);
        Plato saved = platoRepository.save(plato);
        return toDto(requirePlato(saved.getId()));
    }

    public void eliminarLogico(UUID id) {
        Plato plato = requirePlato(id);
        plato.setActivo(false);
        platoRepository.save(plato);
    }

    private Plato requirePlato(UUID id) {
        return platoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plato no encontrado"));
    }

    private Categoria requireCategoria(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
    }

    private void validarCrear(PlatoCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getIdCategoria(), request.getNombre(), request.getPrecio());
        if (platoRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new IllegalStateException("El nombre del plato ya existe");
        }
    }

    private void validarActualizar(UUID id, PlatoUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getIdCategoria(), request.getNombre(), request.getPrecio());
        if (platoRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre().trim(), id)) {
            throw new IllegalStateException("El nombre del plato ya existe");
        }
    }

    private void validarCamposBase(UUID idCategoria, String nombre, Double precio) {
        if (idCategoria == null) {
            throw new IllegalArgumentException("idCategoria es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (precio == null || precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a 0");
        }
    }

    private PlatoDto toDto(Plato plato) {
        PlatoDto dto = new PlatoDto();
        dto.setId(plato.getId());
        dto.setCodigo(plato.getCodigo());
        dto.setIdCategoria(plato.getIdCategoria());
        dto.setCategoria(plato.getCategoria());
        dto.setNombre(plato.getNombre());
        dto.setDescripcion(plato.getDescripcion());
        dto.setPrecio(plato.getPrecio());
        dto.setDisponible(plato.getDisponible());
        dto.setActivo(plato.getActivo());
        dto.setFechaCreacion(plato.getFechaCreacion());
        dto.setFechaActualizacion(plato.getFechaActualizacion());
        return dto;
    }
}
