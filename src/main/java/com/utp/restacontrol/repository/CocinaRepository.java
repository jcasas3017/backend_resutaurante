package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.CocinaItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CocinaRepository {

    private final JdbcTemplate jdbcTemplate;

    public CocinaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CocinaItem> listarItemsCocina() {
        return jdbcTemplate.query(
                """
                SELECT p.codigo AS pedido,
                       m.codigo AS mesa,
                       COALESCE(pl.nombre, pr.nombre, 'Item') AS plato,
                       dp.cantidad,
                       to_char(p.creado_en, 'DD/MM/YYYY HH24:MI') AS hora_envio,
                       CASE dp.estado_cocina::text
                           WHEN 'pendiente' THEN 'Pendiente'
                           WHEN 'en preparacion' THEN 'Pendiente'
                           WHEN 'listo' THEN 'Listo para entrega'
                           WHEN 'despachado' THEN 'Entregado'
                           ELSE 'Pendiente'
                       END AS estado
                FROM pedidos p
                INNER JOIN atenciones a ON a.id = p.id_atencion
                INNER JOIN mesas m ON m.id = a.id_mesa
                INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                LEFT JOIN platos pl ON pl.id = dp.id_plato
                LEFT JOIN productos pr ON pr.id = dp.id_producto
                ORDER BY p.creado_en DESC, dp.fecha_creacion ASC
                """,
                (rs, rowNum) -> new CocinaItem(
                        rs.getString("pedido"),
                        rs.getString("mesa"),
                        rs.getString("plato"),
                        rs.getInt("cantidad"),
                        rs.getString("hora_envio"),
                        rs.getString("estado")
                )
        );
    }
}
