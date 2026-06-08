package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findAllByOrderByNombresAscApellidosAsc();

    boolean existsByDocumento(String documento);

    boolean existsByDocumentoAndIdNot(String documento, UUID id);

    Optional<Cliente> findByDocumento(String documento);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("""
            SELECT c FROM Cliente c
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(CONCAT(
                       COALESCE(c.nombres, ''), ' ',
                       COALESCE(c.apellidos, ''), ' ',
                       COALESCE(c.documento, ''), ' ',
                       COALESCE(c.telefono, ''), ' ',
                       COALESCE(c.email, '')
                   )) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Cliente> buscarClientes(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
