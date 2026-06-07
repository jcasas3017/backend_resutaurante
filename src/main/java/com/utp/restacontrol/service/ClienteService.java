package com.utp.restacontrol.service;

import com.utp.restacontrol.model.Cliente;
import com.utp.restacontrol.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAllByOrderByNombresAscApellidosAsc();
    }
}