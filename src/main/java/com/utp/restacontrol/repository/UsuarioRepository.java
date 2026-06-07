package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByUsernameIgnoreCaseAndActivoTrue(String username);

    List<Usuario> findByActivoTrueOrderByUsernameAsc();

        Optional<Usuario> findById(UUID id);

        boolean existsByUsernameIgnoreCase(String username);

        boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);

        @Query("""
            SELECT u
            FROM Usuario u
            WHERE (:search IS NULL OR :search = '' OR
               LOWER(CONCAT(COALESCE(u.nombres, ''), ' ', COALESCE(u.apellidos, ''), ' ', COALESCE(u.username, ''))) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:rol IS NULL OR :rol = '' OR u.rol = :rol)
              AND (:activo IS NULL OR u.activo = :activo)
            """)
        Page<Usuario> buscarUsuarios(
            @Param("search") String search,
            @Param("rol") String rol,
            @Param("activo") Boolean activo,
            Pageable pageable
        );
}
