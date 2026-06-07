package com.utp.restacontrol.service;

import com.utp.restacontrol.model.Atencion;
import com.utp.restacontrol.repository.AtencionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AtencionService {

    private final AtencionRepository atencionRepository;

    public AtencionService(AtencionRepository atencionRepository) {
        this.atencionRepository = atencionRepository;
    }

    public List<Atencion> listarAtenciones() {
        return atencionRepository.listarAtenciones();
    }
}