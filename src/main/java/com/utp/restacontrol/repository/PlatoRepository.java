package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Plato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlatoRepository extends JpaRepository<Plato, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    @Query("""
        SELECT p FROM Plato p
        JOIN p.categoriaRef c
        WHERE (:search IS NULL OR :search = '' OR
           LOWER(CONCAT(COALESCE(p.nombre, ''), ' ', COALESCE(p.descripcion, ''), ' ', COALESCE(c.nombre, ''))) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:categoriaId IS NULL OR c.id = :categoriaId)
          AND (:activo IS NULL OR p.activo = :activo)
          AND (:disponible IS NULL OR p.disponible = :disponible)
        """)
    Page<Plato> buscarPlatos(
        @Param("search") String search,
        @Param("categoriaId") UUID categoriaId,
        @Param("activo") Boolean activo,
        @Param("disponible") Boolean disponible,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM Plato p
        JOIN p.categoriaRef c
        ORDER BY c.orden, p.nombre
        """)
    List<Plato> findAllOrdenadoVista();
}
