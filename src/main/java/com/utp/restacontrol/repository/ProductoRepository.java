package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    @Query("""
            SELECT p FROM Producto p
            WHERE (:search IS NULL OR :search = '' OR LOWER(CONCAT(COALESCE(p.nombre, ''), ' ', COALESCE(p.descripcion, ''))) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:activo IS NULL OR p.activo = :activo)
            """)
    Page<Producto> buscarProductos(
            @Param("search") String search,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
