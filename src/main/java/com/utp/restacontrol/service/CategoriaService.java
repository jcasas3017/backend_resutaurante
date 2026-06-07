package com.utp.restacontrol.service;

import com.utp.restacontrol.model.Categoria;
import com.utp.restacontrol.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAllByOrderByOrdenAscNombreAsc();
    }
}