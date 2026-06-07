package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Mesa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MesaRepository extends JpaRepository<Mesa, UUID> {

    Mesa findTopByOrderByCodigoDesc();

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, UUID id);

    @Query("""
            SELECT m FROM Mesa m
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(CONCAT(
                       COALESCE(m.codigo, ''), ' ',
                       COALESCE(m.ubicacion, '')
                   )) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:activo IS NULL OR m.activo = :activo)
              AND (:estado IS NULL OR :estado = '' OR LOWER(m.estado) = LOWER(:estado))
            """)
    Page<Mesa> buscarMesas(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            @Param("estado") String estado,
            Pageable pageable
    );
}
