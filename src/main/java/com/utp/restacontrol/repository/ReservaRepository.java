package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @Query("""
            SELECT r FROM Reserva r
            WHERE (:search IS NULL OR :search = ''
                   OR LOWER(CONCAT(
                       COALESCE(r.codigo, ''), ' ',
                       COALESCE(r.tipo, ''), ' ',
                       COALESCE(r.nombreContacto, ''), ' ',
                       COALESCE(r.observacion, ''), ' ',
                       COALESCE(r.estado, '')
                   )) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:estado IS NULL OR :estado = '' OR LOWER(r.estado) = LOWER(:estado))
              AND (:idMesa IS NULL OR r.idMesa = :idMesa)
              AND (:idCliente IS NULL OR r.idCliente = :idCliente)
            """)
    Page<Reserva> buscarReservas(
            @Param("search") String search,
            @Param("estado") String estado,
            @Param("idMesa") UUID idMesa,
            @Param("idCliente") UUID idCliente,
            Pageable pageable
    );

        @Query("""
            SELECT r FROM Reserva r
            WHERE (:search IS NULL OR :search = ''
               OR LOWER(CONCAT(
                   COALESCE(r.codigo, ''), ' ',
                   COALESCE(r.tipo, ''), ' ',
                   COALESCE(r.nombreContacto, ''), ' ',
                   COALESCE(r.observacion, ''), ' ',
                   COALESCE(r.estado, '')
               )) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:estado IS NULL OR :estado = '' OR LOWER(r.estado) = LOWER(:estado))
              AND (:idMesa IS NULL OR r.idMesa = :idMesa)
              AND (:idCliente IS NULL OR r.idCliente = :idCliente)
            ORDER BY r.fechaHora DESC
            """)
        java.util.List<Reserva> exportarReservas(
            @Param("search") String search,
            @Param("estado") String estado,
            @Param("idMesa") UUID idMesa,
            @Param("idCliente") UUID idCliente
        );
}
