package com.utp.restacontrol.service;

import com.utp.restacontrol.model.Plato;
import com.utp.restacontrol.repository.PlatoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatoService {

    private final PlatoRepository platoRepository;

    public PlatoService(PlatoRepository platoRepository) {
        this.platoRepository = platoRepository;
    }

    public List<Plato> listarPlatos() {
        return platoRepository.findAllOrdenadoVista();
    }
}