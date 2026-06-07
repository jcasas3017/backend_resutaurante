package com.utp.restacontrol.repository;

import com.utp.restacontrol.model.Atencion;
import com.utp.restacontrol.model.DetallePedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AtencionRepository {

    private final JdbcTemplate jdbcTemplate;

    public AtencionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Atencion> listarAtenciones() {
        List<AtencionBase> bases = jdbcTemplate.query(
                """
                SELECT a.id,
                       a.codigo,
                       CONCAT(c.nombres, ' ', c.apellidos) AS cliente,
                       m.codigo AS mesa,
                       CONCAT(u.nombres, ' ', u.apellidos) AS mozo,
                       a.estado::text AS estado,
                       a.estado_pago::text AS estado_pago
                FROM atenciones a
                INNER JOIN clientes c ON c.id = a.id_cliente
                INNER JOIN mesas m ON m.id = a.id_mesa
                INNER JOIN usuarios u ON u.id = a.id_mozo
                ORDER BY a.apertura_en DESC
                """,
                (rs, rowNum) -> new AtencionBase(
                        rs.getString("id"),
                        rs.getString("codigo"),
                        rs.getString("cliente"),
                        rs.getString("mesa"),
                        rs.getString("mozo"),
                        rs.getString("estado"),
                        rs.getString("estado_pago")
                )
        );

        List<Atencion> result = new ArrayList<>();
        for (AtencionBase base : bases) {
            List<DetallePedido> detalles = jdbcTemplate.query(
                    """
                    SELECT COALESCE(pl.nombre, pr.nombre, 'Item') AS plato,
                           dp.cantidad,
                           dp.precio_unit,
                           dp.descuento
                    FROM pedidos p
                    INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                    LEFT JOIN platos pl ON pl.id = dp.id_plato
                    LEFT JOIN productos pr ON pr.id = dp.id_producto
                    WHERE p.id_atencion = ?::uuid
                    ORDER BY p.creado_en, dp.fecha_creacion
                    """,
                    (rs, rowNum) -> {
                        int cantidad = rs.getInt("cantidad");
                        double precioUnit = rs.getDouble("precio_unit");
                        double descuento = rs.getDouble("descuento");
                        double descuentoUnit = cantidad > 0 ? (descuento / cantidad) : 0;
                        return new DetallePedido(
                                rs.getString("plato"),
                                cantidad,
                                Math.max(precioUnit - descuentoUnit, 0)
                        );
                    },
                    base.id()
            );

            result.add(new Atencion(
                    base.codigo(),
                    base.cliente(),
                    base.mesa(),
                    base.mozo(),
                    base.estado(),
                    base.estadoPago(),
                    detalles
            ));
        }

        return result;
    }

    private record AtencionBase(
            String id,
            String codigo,
            String cliente,
            String mesa,
            String mozo,
            String estado,
            String estadoPago
    ) {
    }
}
