package com.utp.restacontrol.service;

import com.utp.restacontrol.model.CocinaItem;
import com.utp.restacontrol.repository.CocinaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CocinaService {

    private final CocinaRepository cocinaRepository;

    public CocinaService(CocinaRepository cocinaRepository) {
        this.cocinaRepository = cocinaRepository;
    }

    public List<Map<String, Object>> listarItemsCocina(String estado, String search) {
        return cocinaRepository.listarItemsCocina(estado, search);
    }

    public List<CocinaItem> listarItemsCocina() {
        return cocinaRepository.listarItemsCocina();
    }
}