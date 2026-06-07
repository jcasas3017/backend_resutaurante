package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.producto.ProductoCreateRequest;
import com.utp.restacontrol.dto.producto.ProductoDto;
import com.utp.restacontrol.dto.producto.ProductoUpdateRequest;
import com.utp.restacontrol.model.Producto;
import com.utp.restacontrol.model.Unidad;
import com.utp.restacontrol.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductoCrudService {

    private final ProductoRepository productoRepository;

    public ProductoCrudService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Page<ProductoDto> listar(String search, Boolean activo, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "nombre"));
        return productoRepository.buscarProductos(search, activo, pageable).map(this::toDto);
    }

    public ProductoDto obtener(UUID id) {
        return toDto(requireProducto(id));
    }

    public ProductoDto crear(ProductoCreateRequest request) {
        validarCrear(request);

        Producto producto = new Producto();
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setStockMinimo(request.getStockMinimo() == null ? 10 : request.getStockMinimo());
        producto.setUnidad(parseUnidad(request.getUnidad()));
        producto.setActivo(request.getActivo() == null || request.getActivo());

        Producto saved = productoRepository.save(producto);
        return toDto(requireProducto(saved.getId()));
    }

    public ProductoDto actualizar(UUID id, ProductoUpdateRequest request) {
        validarActualizar(id, request);

        Producto producto = requireProducto(id);
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setStockMinimo(request.getStockMinimo() == null ? 10 : request.getStockMinimo());
        producto.setUnidad(parseUnidad(request.getUnidad()));
        producto.setActivo(request.getActivo() == null || request.getActivo());

        Producto saved = productoRepository.save(producto);
        return toDto(requireProducto(saved.getId()));
    }

    public ProductoDto cambiarEstado(UUID id, boolean activo) {
        Producto producto = requireProducto(id);
        producto.setActivo(activo);
        Producto saved = productoRepository.save(producto);
        return toDto(requireProducto(saved.getId()));
    }

    public void eliminarLogico(UUID id) {
        Producto producto = requireProducto(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private Producto requireProducto(UUID id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    private void validarCrear(ProductoCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombre(), request.getPrecio(), request.getStock(), request.getUnidad());
        if (productoRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new IllegalStateException("El nombre del producto ya existe");
        }
    }

    private void validarActualizar(UUID id, ProductoUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(request.getNombre(), request.getPrecio(), request.getStock(), request.getUnidad());
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre().trim(), id)) {
            throw new IllegalStateException("El nombre del producto ya existe");
        }
    }

    private void validarCamposBase(String nombre, Double precio, Integer stock, String unidad) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (precio == null || precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a 0");
        }
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        parseUnidad(unidad);
    }

    private Unidad parseUnidad(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return Unidad.unidad;
        }

        String normalized = Normalizer.normalize(unidad.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "unidad", "unidades", "botella", "botellas", "lata", "latas", "pieza", "piezas", "pz" -> Unidad.unidad;
            case "litro", "litros", "l" -> Unidad.litro;
            case "kg", "kilo", "kilos" -> Unidad.kg;
            case "gramo", "gramos", "gr" -> Unidad.gramo;
            default -> throw new IllegalArgumentException("Unidad no válida. Usa: unidad, litro, kg, gramo");
        };
    }

    private ProductoDto toDto(Producto producto) {
        ProductoDto dto = new ProductoDto();
        dto.setId(producto.getId());
        dto.setCodigo(producto.getCodigo());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        dto.setStockMinimo(producto.getStockMinimo());
        dto.setUnidad(producto.getUnidad() != null ? producto.getUnidad().name() : null);
        dto.setActivo(producto.getActivo());
        dto.setFechaCreacion(producto.getFechaCreacion());
        dto.setFechaActualizacion(producto.getFechaActualizacion());
        return dto;
    }
}
