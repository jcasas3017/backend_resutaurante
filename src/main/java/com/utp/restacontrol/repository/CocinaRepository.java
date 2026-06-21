package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.CocinaItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class CocinaRepository {

    private final JdbcTemplate jdbcTemplate;

    public CocinaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listarItemsCocina(String estado, String search) {
        StringBuilder sql = new StringBuilder(
                """
                        SELECT dp.id AS detalle_id,
                               p.id AS pedido_id,
                               m.codigo AS mesa_codigo,
                               pl.nombre AS plato_nombre,
                               dp.cantidad,
                               dp.fecha_creacion AS creado_en,
                               dp.estado_cocina AS estado_cocina,
                               COALESCE(dp.observaciones, '') AS observaciones
                        FROM pedidos p
                        INNER JOIN atenciones a ON a.id = p.id_atencion
                        INNER JOIN mesas m ON m.id = a.id_mesa
                        INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                        LEFT JOIN platos pl ON pl.id = dp.id_plato
                        WHERE dp.id_plato IS NOT NULL
                        """);

        List<Object> params = new ArrayList<>();

        if (estado != null && !estado.isBlank()) {
            String estadoNormalized = estado.trim().toLowerCase(Locale.ROOT);
            if (!"todos".equals(estadoNormalized)) {
                sql.append(" AND dp.estado_cocina = ?");
                params.add(estadoNormalized);
            }
        }

        if (search != null && !search.isBlank()) {
            String texto = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            sql.append(
                    " AND (LOWER(pl.nombre) LIKE ? OR LOWER(m.codigo) LIKE ? OR LOWER(p.codigo) LIKE ? OR LOWER(COALESCE(dp.observaciones, '')) LIKE ?)");
            params.add(texto);
            params.add(texto);
            params.add(texto);
            params.add(texto);
        }

        sql.append(" ORDER BY dp.fecha_creacion ASC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("creado_en");
            OffsetDateTime creadoEn = timestamp != null ? timestamp.toInstant().atOffset(ZoneOffset.UTC) : null;
            Map<String, Object> row = new HashMap<>();
            row.put("detalleId", rs.getObject("detalle_id"));
            row.put("pedidoId", rs.getObject("pedido_id"));
            row.put("mesaCodigo", rs.getString("mesa_codigo"));
            row.put("platoNombre", rs.getString("plato_nombre"));
            row.put("cantidad", rs.getInt("cantidad"));
            row.put("creadoEn", creadoEn);
            row.put("estadoCocina", rs.getString("estado_cocina"));
            row.put("observaciones", rs.getString("observaciones"));
            return row;
        });
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
                        WHERE dp.id_plato IS NOT NULL
                          AND dp.estado_cocina::text <> 'cancelado'
                        ORDER BY p.creado_en DESC, dp.fecha_creacion ASC
                        """,
                (rs, rowNum) -> new CocinaItem(
                        rs.getString("pedido"),
                        rs.getString("mesa"),
                        rs.getString("plato"),
                        rs.getInt("cantidad"),
                        rs.getString("hora_envio"),
                        rs.getString("estado")));
    }
}
