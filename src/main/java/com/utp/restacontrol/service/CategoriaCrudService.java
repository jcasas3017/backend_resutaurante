package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.categoria.CategoriaCreateRequest;
import com.utp.restacontrol.dto.categoria.CategoriaDto;
import com.utp.restacontrol.dto.categoria.CategoriaUpdateRequest;
import com.utp.restacontrol.model.Categoria;
import com.utp.restacontrol.repository.CategoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CategoriaCrudService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaCrudService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public Page<CategoriaDto> listar(String search, Boolean activo, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "orden", "nombre"));
        return categoriaRepository.buscarCategorias(search, activo, pageable).map(this::toDto);
    }

    public CategoriaDto obtener(UUID id) {
        return toDto(requireCategoria(id));
    }

    public CategoriaDto crear(CategoriaCreateRequest request) {
        validarCrear(request);

        Categoria categoria = new Categoria();
        categoria.setNombre(request.getNombre().trim());
        categoria.setOrden(request.getOrden());
        categoria.setActivo(request.getActivo() == null || request.getActivo());

        Categoria saved = categoriaRepository.save(categoria);
        return toDto(requireCategoria(saved.getId()));
    }

    public CategoriaDto actualizar(UUID id, CategoriaUpdateRequest request) {
        validarActualizar(id, request);

        Categoria categoria = requireCategoria(id);
        categoria.setNombre(request.getNombre().trim());
        categoria.setOrden(request.getOrden());
        categoria.setActivo(request.getActivo() == null || request.getActivo());

        Categoria saved = categoriaRepository.save(categoria);
        return toDto(requireCategoria(saved.getId()));
    }

    public CategoriaDto cambiarEstado(UUID id, boolean activo) {
        Categoria categoria = requireCategoria(id);
        categoria.setActivo(activo);
        Categoria saved = categoriaRepository.save(categoria);
        return toDto(requireCategoria(saved.getId()));
    }

    public void eliminarLogico(UUID id) {
        Categoria categoria = requireCategoria(id);
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    private Categoria requireCategoria(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
    }

    private void validarCrear(CategoriaCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombre(), request.getOrden());
        if (categoriaRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new IllegalStateException("El nombre de categoría ya existe");
        }
    }

    private void validarActualizar(UUID id, CategoriaUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombre(), request.getOrden());
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre().trim(), id)) {
            throw new IllegalStateException("El nombre de categoría ya existe");
        }
    }

    private void validarCamposBase(String nombre, Integer orden) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (orden == null || orden < 1) {
            throw new IllegalArgumentException("El orden debe ser mayor a 0");
        }
    }

    private CategoriaDto toDto(Categoria categoria) {
        CategoriaDto dto = new CategoriaDto();
        dto.setId(categoria.getId());
        dto.setCodigo(categoria.getCodigo());
        dto.setNombre(categoria.getNombre());
        dto.setOrden(categoria.getOrden());
        dto.setActivo(categoria.getActivo());
        dto.setFechaCreacion(categoria.getFechaCreacion());
        dto.setFechaActualizacion(categoria.getFechaActualizacion());
        return dto;
    }
}
