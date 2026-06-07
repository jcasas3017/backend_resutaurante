package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    List<Categoria> findAllByOrderByOrdenAscNombreAsc();

    @Query("""
            SELECT c
            FROM Categoria c
            WHERE (:search IS NULL OR :search = '' OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:activo IS NULL OR c.activo = :activo)
            """)
    Page<Categoria> buscarCategorias(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
