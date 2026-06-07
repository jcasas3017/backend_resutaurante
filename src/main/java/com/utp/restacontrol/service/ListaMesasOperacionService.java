package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.operacion.AgregarItemRequest;
import com.utp.restacontrol.dto.operacion.CambiarEstadoItemRequest;
import com.utp.restacontrol.dto.operacion.CobrarAtencionRequest;
import com.utp.restacontrol.dto.operacion.OcuparMesaRequest;
import com.utp.restacontrol.dto.operacion.OperacionItemRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ListaMesasOperacionService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private final JdbcTemplate jdbcTemplate;

    public ListaMesasOperacionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> tablero(LocalDateTime fechaHora) {
        LocalDateTime ref = fechaHora != null ? fechaHora : LocalDateTime.now(LIMA_ZONE);

        List<Map<String, Object>> rows = jdbcTemplate.query(
                """
                SELECT m.id,
                       m.codigo,
                       m.capacidad,
                       m.ubicacion,
                       m.activa,
                       COALESCE(m.estado::text, 'disponible') AS situacion,
                       r.id AS id_reserva,
                       r.id_cliente AS reserva_id_cliente,
                       r.nombre_contacto AS reserva_nombre_contacto,
                       r.fecha_hora AS reserva_fecha_hora,
                       r.cantidad_personas AS reserva_cantidad_personas,
                       r.estado::text AS reserva_estado,
                       r.confirmada AS reserva_confirmada,
                       a.id AS id_atencion,
                       a.id_cliente AS atencion_id_cliente,
                       a.id_mozo AS atencion_id_mozo,
                       a.apertura_en,
                       a.estado_pago::text AS estado_pago,
                       COALESCE(t.total_actual, 0) AS total_actual
                FROM mesas m
                LEFT JOIN LATERAL (
                    SELECT rv.*
                    FROM reservas rv
                    WHERE rv.id_mesa = m.id
                      AND rv.estado::text IN ('pendiente', 'confirmada')
                      AND rv.fecha_hora BETWEEN (?::timestamp - INTERVAL '2 hours') AND (?::timestamp + INTERVAL '6 hours')
                    ORDER BY rv.confirmada DESC, rv.fecha_hora ASC
                    LIMIT 1
                ) r ON TRUE
                LEFT JOIN LATERAL (
                    SELECT at.*
                    FROM atenciones at
                    WHERE at.id_mesa = m.id
                      AND at.estado::text = 'en_curso'
                    ORDER BY at.apertura_en DESC
                    LIMIT 1
                ) a ON TRUE
                LEFT JOIN LATERAL (
                    SELECT SUM((dp.cantidad * dp.precio_unit) - COALESCE(dp.descuento, 0)) AS total_actual
                    FROM pedidos p
                    INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                    WHERE p.id_atencion = a.id
                      AND dp.estado_cocina::text <> 'cancelado'
                ) t ON TRUE
                ORDER BY m.codigo ASC
                """,
                (rs, rowNum) -> {
                    Map<String, Object> item = new HashMap<>();

                    UUID idMesa = (UUID) rs.getObject("id");
                    boolean activa = rs.getBoolean("activa");
                    String situacion = text(rs.getString("situacion"));
                    UUID idAtencion = (UUID) rs.getObject("id_atencion");
                    UUID idReserva = (UUID) rs.getObject("id_reserva");

                    item.put("idMesa", idMesa);
                    item.put("codigo", rs.getString("codigo"));
                    item.put("capacidad", rs.getInt("capacidad"));
                    item.put("ubicacion", rs.getString("ubicacion"));
                    item.put("activa", activa);
                    item.put("estadoOperativo", calcularEstadoOperativo(activa, situacion, idAtencion != null, idReserva != null));
                    item.put("reservaActiva", buildReservaActiva(rs, idReserva));
                    item.put("atencionActiva", buildAtencionActiva(rs, idAtencion));
                    item.put("totalActual", rs.getDouble("total_actual"));
                    return item;
                },
                Timestamp.valueOf(ref),
                Timestamp.valueOf(ref)
        );

        return rows;
    }

    public Map<String, Object> contexto(UUID idMesa, LocalDateTime fechaHora) {
        LocalDateTime ref = fechaHora != null ? fechaHora : LocalDateTime.now(LIMA_ZONE);

        Map<String, Object> mesa = jdbcTemplate.query(
                """
                SELECT m.id,
                       m.codigo,
                       m.capacidad,
                       m.ubicacion,
                       m.activa,
                       COALESCE(m.estado::text, 'disponible') AS situacion
                FROM mesas m
                WHERE m.id = ?::uuid
                """,
                rs -> rs.next() ? Map.of(
                        "idMesa", rs.getObject("id"),
                        "codigo", rs.getString("codigo"),
                        "capacidad", rs.getInt("capacidad"),
                        "ubicacion", rs.getString("ubicacion"),
                        "activa", rs.getBoolean("activa"),
                        "situacion", rs.getString("situacion")
                ) : null,
                idMesa
        );

        if (mesa == null) {
            throw new OperacionBusinessException("Mesa no encontrada", "VALIDACION_NEGOCIO");
        }

        Map<String, Object> reservaActiva = jdbcTemplate.query(
                """
                SELECT r.id,
                       r.id_cliente,
                       r.nombre_contacto,
                       r.fecha_hora,
                       r.cantidad_personas,
                       r.estado::text AS estado,
                       r.confirmada
                FROM reservas r
                WHERE r.id_mesa = ?::uuid
                  AND r.estado::text IN ('pendiente', 'confirmada')
                  AND r.fecha_hora BETWEEN (?::timestamp - INTERVAL '2 hours') AND (?::timestamp + INTERVAL '6 hours')
                ORDER BY r.confirmada DESC, r.fecha_hora ASC
                LIMIT 1
                """,
                rs -> rs.next() ? Map.of(
                        "idReserva", rs.getObject("id"),
                        "idCliente", rs.getObject("id_cliente"),
                        "nombreContacto", rs.getString("nombre_contacto"),
                        "fechaHora", rs.getTimestamp("fecha_hora").toLocalDateTime(),
                        "cantidadPersonas", rs.getInt("cantidad_personas"),
                        "estado", rs.getString("estado"),
                        "confirmada", rs.getBoolean("confirmada")
                ) : null,
                idMesa,
                Timestamp.valueOf(ref),
                Timestamp.valueOf(ref)
        );

        Map<String, Object> atencionActiva = jdbcTemplate.query(
                """
                SELECT a.id,
                       a.id_cliente,
                       a.id_mozo,
                       a.apertura_en,
                       a.estado_pago::text AS estado_pago
                FROM atenciones a
                WHERE a.id_mesa = ?::uuid
                  AND a.estado::text = 'en_curso'
                ORDER BY a.apertura_en DESC
                LIMIT 1
                """,
                rs -> rs.next() ? Map.of(
                        "idAtencion", rs.getObject("id"),
                        "idCliente", rs.getObject("id_cliente"),
                        "idMozo", rs.getObject("id_mozo"),
                        "aperturaEn", rs.getTimestamp("apertura_en").toLocalDateTime(),
                        "estadoPago", rs.getString("estado_pago")
                ) : null,
                idMesa
        );

        Map<String, Object> pedidoActual = null;
        if (atencionActiva != null) {
            UUID idAtencion = (UUID) atencionActiva.get("idAtencion");
            pedidoActual = construirPedidoActual(idAtencion);
        }

        return Map.of(
                "mesa", mesa,
                "reservaActiva", reservaActiva,
                "atencionActiva", atencionActiva,
                "pedidoActual", pedidoActual
        );
    }

    @Transactional
    public Map<String, Object> ocuparMesa(UUID idMesa, UUID idReserva, OcuparMesaRequest request) {
        validarOcuparRequest(request);

        Map<String, Object> mesaLock = jdbcTemplate.query(
                """
                SELECT id, activa, COALESCE(estado::text, 'disponible') AS situacion
                FROM mesas
                WHERE id = ?::uuid
                FOR UPDATE
                """,
                rs -> rs.next() ? Map.of(
                        "id", rs.getObject("id"),
                        "activa", rs.getBoolean("activa"),
                        "situacion", rs.getString("situacion")
                ) : null,
                idMesa
        );

        if (mesaLock == null) {
            throw new OperacionBusinessException("Mesa no disponible", "MESA_NO_DISPONIBLE");
        }

        boolean activa = Boolean.TRUE.equals(mesaLock.get("activa"));
        String situacion = text((String) mesaLock.get("situacion"));
        if (!activa || "mantenimiento".equalsIgnoreCase(situacion)) {
            throw new OperacionBusinessException("Mesa no disponible", "MESA_NO_DISPONIBLE");
        }

        Integer abiertas = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM atenciones
                WHERE id_mesa = ?::uuid
                  AND estado::text = 'en_curso'
                """,
                Integer.class,
                idMesa
        );

        if (abiertas != null && abiertas > 0) {
            throw new OperacionBusinessException("Mesa ya ocupada", "MESA_OCUPADA");
        }

        if (idReserva != null) {
            Integer vigente = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(1)
                    FROM reservas
                    WHERE id = ?::uuid
                      AND id_mesa = ?::uuid
                      AND estado::text IN ('pendiente', 'confirmada')
                    """,
                    Integer.class,
                    idReserva,
                    idMesa
            );
            if (vigente == null || vigente == 0) {
                throw new OperacionBusinessException("Reserva no vigente", "RESERVA_NO_VIGENTE");
            }
        }

        UUID idAtencion = insertarAtencion(idMesa, idReserva, request);
        UUID idPedido = insertarPedido(idAtencion, request.getNotas());

        for (OperacionItemRequest item : request.getItems()) {
            insertarDetalleItem(idPedido, item, request.getIdMozo());
        }

        if (idReserva != null) {
            jdbcTemplate.update(
                    """
                    UPDATE reservas
                    SET estado = 'confirmada'::estado_reserva,
                        confirmada = true
                    WHERE id = ?::uuid
                    """,
                    idReserva
            );
        }

        return contexto(idMesa, LocalDateTime.now(LIMA_ZONE));
    }

    @Transactional
    public Map<String, Object> agregarItem(UUID idAtencion, AgregarItemRequest request) {
        if (request == null || request.getTipoItem() == null || request.getTipoItem().isBlank() || request.getIdItem() == null || request.getCantidad() == null || request.getCantidad() < 1) {
            throw new OperacionBusinessException("Item no valido", "ITEM_NO_VALIDO");
        }

        UUID idPedido = jdbcTemplate.query(
                """
                SELECT p.id
                FROM pedidos p
                WHERE p.id_atencion = ?::uuid
                ORDER BY p.creado_en DESC
                LIMIT 1
                """,
                rs -> rs.next() ? (UUID) rs.getObject("id") : null,
                idAtencion
        );

        if (idPedido == null) {
            idPedido = insertarPedido(idAtencion, null);
        }

        UUID idDetalle = insertarDetalleItem(idPedido, toOperacionItem(request), null);

        return Map.of(
                "idDetalle", idDetalle,
                "idPedido", idPedido
        );
    }

    @Transactional
    public Map<String, Object> cambiarEstadoDetalle(UUID idDetalle, CambiarEstadoItemRequest request) {
        if (request == null || request.getEstadoCocina() == null || request.getEstadoCocina().isBlank()) {
            throw new OperacionBusinessException("Transicion de estado invalida", "TRANSICION_ESTADO_INVALIDA");
        }

        Map<String, Object> detalle = jdbcTemplate.query(
                """
                SELECT id, estado_cocina::text AS estado_cocina
                FROM detalle_pedidos
                WHERE id = ?::uuid
                FOR UPDATE
                """,
                rs -> rs.next() ? Map.of(
                        "id", rs.getObject("id"),
                        "estado", rs.getString("estado_cocina")
                ) : null,
                idDetalle
        );

        if (detalle == null) {
            throw new OperacionBusinessException("Item no valido", "ITEM_NO_VALIDO");
        }

        String actual = text((String) detalle.get("estado"));
        String nuevo = text(request.getEstadoCocina());

        if (!transicionValida(actual, nuevo)) {
            throw new OperacionBusinessException("Transicion de estado invalida", "TRANSICION_ESTADO_INVALIDA");
        }

        jdbcTemplate.update(
                """
                UPDATE detalle_pedidos
                SET estado_cocina = ?::estado_cocina
                WHERE id = ?::uuid
                """,
                nuevo,
                idDetalle
        );

        return Map.of(
                "idDetalle", idDetalle,
                "estadoCocina", nuevo
        );
    }

    @Transactional
    public Map<String, Object> cobrarAtencion(UUID idAtencion, CobrarAtencionRequest request) {
        if (request == null || request.getMetodoPago() == null || request.getMetodoPago().isBlank()) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }

        Map<String, Object> atencion = jdbcTemplate.query(
                """
                SELECT id, codigo, id_cliente, id_mesa, id_reserva, estado::text AS estado
                FROM atenciones
                WHERE id = ?::uuid
                FOR UPDATE
                """,
                rs -> rs.next() ? Map.of(
                        "id", rs.getObject("id"),
                        "codigo", rs.getString("codigo"),
                        "idCliente", rs.getObject("id_cliente"),
                        "idMesa", rs.getObject("id_mesa"),
                        "idReserva", rs.getObject("id_reserva"),
                        "estado", rs.getString("estado")
                ) : null,
                idAtencion
        );

        if (atencion == null || !"en_curso".equalsIgnoreCase(text((String) atencion.get("estado")))) {
            throw new OperacionBusinessException("Atencion no en curso", "ATENCION_NO_EN_CURSO");
        }

        Double subtotal = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM((dp.cantidad * dp.precio_unit) - COALESCE(dp.descuento, 0)), 0)
                FROM pedidos p
                INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                WHERE p.id_atencion = ?::uuid
                  AND dp.estado_cocina::text <> 'cancelado'
                """,
                Double.class,
                idAtencion
        );

        double propina = request.getPropina() == null ? 0d : Math.max(request.getPropina(), 0d);
        double total = (subtotal == null ? 0d : subtotal) + propina;

        Map<String, Object> comprobanteData = null;
        if (Boolean.TRUE.equals(request.getGenerarComprobante())) {
            comprobanteData = crearComprobante(idAtencion, request.getMetodoPago(), subtotal == null ? 0d : subtotal, propina, total);
        }

        jdbcTemplate.update(
                """
                UPDATE atenciones
                SET estado = 'cerrada'::estado_atencion,
                    estado_pago = 'pagado'::estado_pago,
                    cierre_en = now()
                WHERE id = ?::uuid
                """,
                idAtencion
        );

        UUID idReserva = (UUID) atencion.get("idReserva");
        if (idReserva != null) {
            jdbcTemplate.update(
                    """
                    UPDATE reservas
                    SET estado = 'atendida'::estado_reserva,
                        confirmada = true
                    WHERE id = ?::uuid
                    """,
                    idReserva
            );
        }

        Map<String, Object> result = new HashMap<>();
        if (comprobanteData != null) {
            result.putAll(comprobanteData);
        }
        result.put("subtotal", subtotal == null ? 0d : subtotal);
        result.put("propina", propina);
        result.put("total", total);
        return result;
    }

    private Map<String, Object> crearComprobante(UUID idAtencion, String metodoPago, double subtotal, double propina, double total) {
        String serie = "B001";

        Integer maxCorr = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(MAX(CAST(c.correlativo AS integer)), 0)
                FROM comprobantes c
                WHERE c.serie = ?
                """,
                Integer.class,
                serie
        );

        String correlativo = String.format(Locale.ROOT, "%08d", (maxCorr == null ? 0 : maxCorr) + 1);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO comprobantes (
                        id_atencion, serie, correlativo, tipo_comprobante,
                        subtotal, propina, total, metodo_pago, fecha_emision, estado
                    )
                    VALUES (?::uuid, ?, ?, 'boleta', ?, ?, ?, ?, now(), 'emitido')
                    RETURNING id
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setObject(1, idAtencion);
            ps.setString(2, serie);
            ps.setString(3, correlativo);
            ps.setDouble(4, subtotal);
            ps.setDouble(5, propina);
            ps.setDouble(6, total);
            ps.setString(7, metodoPago);
            return ps;
        }, keyHolder);

        Object idComprobante = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : null;

        return Map.of(
                "idComprobante", idComprobante,
                "serie", serie,
                "correlativo", correlativo,
                "fechaEmision", LocalDateTime.now(LIMA_ZONE),
                "metodoPago", metodoPago
        );
    }

    private UUID insertarAtencion(UUID idMesa, UUID idReserva, OcuparMesaRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO atenciones (
                        id_mesa, id_cliente, id_mozo, id_reserva,
                        estado, estado_pago, apertura_en, observaciones
                    )
                    VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid,
                            'en_curso'::estado_atencion, 'pendiente'::estado_pago, now(), ?)
                    RETURNING id
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setObject(1, idMesa);
            ps.setObject(2, request.getIdCliente());
            ps.setObject(3, request.getIdMozo());
            ps.setObject(4, idReserva);
            ps.setString(5, trimToNull(request.getNotas()));
            return ps;
        }, keyHolder);

        Object id = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : null;
        if (!(id instanceof UUID uuid)) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }
        return uuid;
    }

    private UUID insertarPedido(UUID idAtencion, String notas) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO pedidos (id_atencion, notas, creado_en)
                    VALUES (?::uuid, ?, now())
                    RETURNING id
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setObject(1, idAtencion);
            ps.setString(2, trimToNull(notas));
            return ps;
        }, keyHolder);

        Object id = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : null;
        if (!(id instanceof UUID uuid)) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }
        return uuid;
    }

    private UUID insertarDetalleItem(UUID idPedido, OperacionItemRequest item, UUID idUsuario) {
        String tipo = text(item.getTipoItem());
        if (!"plato".equals(tipo) && !"producto".equals(tipo)) {
            throw new OperacionBusinessException("Item no valido", "ITEM_NO_VALIDO");
        }

        if (item.getIdItem() == null || item.getCantidad() == null || item.getCantidad() < 1) {
            throw new OperacionBusinessException("Item no valido", "ITEM_NO_VALIDO");
        }

        Double precio;
        UUID idPlato = null;
        UUID idProducto = null;
        String estadoCocina;

        if ("plato".equals(tipo)) {
            Map<String, Object> plato = jdbcTemplate.query(
                    """
                    SELECT id, precio, disponible, activo
                    FROM platos
                    WHERE id = ?::uuid
                    """,
                    rs -> rs.next() ? Map.of(
                            "id", rs.getObject("id"),
                            "precio", rs.getDouble("precio"),
                            "disponible", rs.getBoolean("disponible"),
                            "activo", rs.getBoolean("activo")
                    ) : null,
                    item.getIdItem()
            );

            if (plato == null || !Boolean.TRUE.equals(plato.get("disponible")) || !Boolean.TRUE.equals(plato.get("activo"))) {
                throw new OperacionBusinessException("Item no valido", "ITEM_NO_VALIDO");
            }
            idPlato = (UUID) plato.get("id");
            precio = (Double) plato.get("precio");
            estadoCocina = "pendiente";
        } else {
            Map<String, Object> producto = jdbcTemplate.query(
                    """
                    SELECT id, precio, stock, activo
                    FROM productos
                    WHERE id = ?::uuid
                    FOR UPDATE
                    """,
                    rs -> rs.next() ? Map.of(
                            "id", rs.getObject("id"),
                            "precio", rs.getDouble("precio"),
                            "stock", rs.getInt("stock"),
                            "activo", rs.getBoolean("activo")
                    ) : null,
                    item.getIdItem()
            );

            if (producto == null || !Boolean.TRUE.equals(producto.get("activo"))) {
                throw new OperacionBusinessException("Item no valido", "ITEM_NO_VALIDO");
            }

            int stock = (Integer) producto.get("stock");
            if (stock < item.getCantidad()) {
                throw new OperacionBusinessException("Stock insuficiente", "STOCK_INSUFICIENTE");
            }

            idProducto = (UUID) producto.get("id");
            precio = (Double) producto.get("precio");
            estadoCocina = "entregado";

            jdbcTemplate.update(
                    """
                    UPDATE productos
                    SET stock = stock - ?
                    WHERE id = ?::uuid
                    """,
                    item.getCantidad(),
                    idProducto
            );

            tryInsertMovimientoStock(idProducto, item.getCantidad(), idUsuario);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        UUID finalIdPlato = idPlato;
        UUID finalIdProducto = idProducto;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO detalle_pedidos (
                        id_pedido, id_plato, id_producto, cantidad,
                        precio_unit, descuento, estado_cocina, observaciones, fecha_creacion
                    )
                    VALUES (?::uuid, ?::uuid, ?::uuid, ?,
                            ?, 0, ?::estado_cocina, ?, now())
                    RETURNING id
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setObject(1, idPedido);
            ps.setObject(2, finalIdPlato);
            ps.setObject(3, finalIdProducto);
            ps.setInt(4, item.getCantidad());
            ps.setDouble(5, precio == null ? 0d : precio);
            ps.setString(6, estadoCocina);
            ps.setString(7, trimToNull(item.getObservaciones()));
            return ps;
        }, keyHolder);

        Object id = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : null;
        if (!(id instanceof UUID uuid)) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }
        return uuid;
    }

    private void tryInsertMovimientoStock(UUID idProducto, int cantidad, UUID idUsuario) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO movimientos_stock (
                        id_producto, tipo_movimiento, cantidad,
                        referencia_tipo, referencia_id, motivo, created_by, created_at
                    )
                    VALUES (?::uuid, 'salida_venta', ?,
                            'detalle_pedido', NULL, 'Salida por venta', ?::uuid, now())
                    """,
                    idProducto,
                    cantidad,
                    idUsuario
            );
        } catch (Exception ignored) {
            // Optional table/columns across environments.
        }
    }

    private Map<String, Object> construirPedidoActual(UUID idAtencion) {
        Map<String, Object> pedido = jdbcTemplate.query(
                """
                SELECT p.id
                FROM pedidos p
                WHERE p.id_atencion = ?::uuid
                ORDER BY p.creado_en DESC
                LIMIT 1
                """,
                rs -> rs.next() ? Map.of("idPedido", rs.getObject("id")) : null,
                idAtencion
        );

        if (pedido == null) {
            return null;
        }

        UUID idPedido = (UUID) pedido.get("idPedido");

        List<Map<String, Object>> items = jdbcTemplate.query(
                """
                SELECT dp.id,
                       CASE WHEN dp.id_plato IS NOT NULL THEN 'plato' ELSE 'producto' END AS tipo_item,
                       COALESCE(dp.id_plato, dp.id_producto) AS id_item,
                       COALESCE(pl.nombre, pr.nombre, 'Item') AS nombre_item,
                       dp.cantidad,
                       dp.precio_unit,
                       dp.descuento,
                       dp.estado_cocina::text AS estado_cocina
                FROM detalle_pedidos dp
                LEFT JOIN platos pl ON pl.id = dp.id_plato
                LEFT JOIN productos pr ON pr.id = dp.id_producto
                WHERE dp.id_pedido = ?::uuid
                ORDER BY dp.fecha_creacion ASC
                """,
                (rs, rowNum) -> {
                    int cantidad = rs.getInt("cantidad");
                    double precioUnit = rs.getDouble("precio_unit");
                    double descuento = rs.getDouble("descuento");
                    return Map.of(
                            "idDetalle", rs.getObject("id"),
                            "tipoItem", rs.getString("tipo_item"),
                            "idItem", rs.getObject("id_item"),
                            "nombreItem", rs.getString("nombre_item"),
                            "cantidad", cantidad,
                            "precioUnit", precioUnit,
                            "descuento", descuento,
                            "estadoCocina", rs.getString("estado_cocina"),
                            "subtotal", (cantidad * precioUnit) - descuento
                    );
                },
                idPedido
        );

        double subtotal = items.stream()
                .mapToDouble(item -> ((Number) item.get("subtotal")).doubleValue())
                .sum();

        Map<String, Object> data = new HashMap<>();
        data.put("idPedido", idPedido);
        data.put("items", items);
        data.put("subtotal", subtotal);
        data.put("propina", 0d);
        data.put("total", subtotal);
        return data;
    }

    private OperacionItemRequest toOperacionItem(AgregarItemRequest request) {
        OperacionItemRequest item = new OperacionItemRequest();
        item.setTipoItem(request.getTipoItem());
        item.setIdItem(request.getIdItem());
        item.setCantidad(request.getCantidad());
        item.setObservaciones(request.getObservaciones());
        return item;
    }

    private String text(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");
    }

    private String calcularEstadoOperativo(boolean activa, String situacion, boolean ocupada, boolean reservada) {
        if (!activa || "mantenimiento".equalsIgnoreCase(situacion)) {
            return "no_disponible";
        }
        if (ocupada) {
            return "ocupada";
        }
        if (reservada) {
            return "reservada";
        }
        return "libre";
    }

    private Object buildReservaActiva(java.sql.ResultSet rs, UUID idReserva) throws java.sql.SQLException {
        if (idReserva == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("idReserva", idReserva);
        data.put("idCliente", rs.getObject("reserva_id_cliente"));
        data.put("nombreContacto", rs.getString("reserva_nombre_contacto"));
        Timestamp fecha = rs.getTimestamp("reserva_fecha_hora");
        data.put("fechaHora", fecha != null ? fecha.toLocalDateTime() : null);
        data.put("cantidadPersonas", rs.getInt("reserva_cantidad_personas"));
        data.put("estado", rs.getString("reserva_estado"));
        data.put("confirmada", rs.getBoolean("reserva_confirmada"));
        return data;
    }

    private Object buildAtencionActiva(java.sql.ResultSet rs, UUID idAtencion) throws java.sql.SQLException {
        if (idAtencion == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("idAtencion", idAtencion);
        data.put("idCliente", rs.getObject("atencion_id_cliente"));
        data.put("idMozo", rs.getObject("atencion_id_mozo"));
        Timestamp apertura = rs.getTimestamp("apertura_en");
        data.put("aperturaEn", apertura != null ? apertura.toLocalDateTime() : null);
        data.put("estadoPago", rs.getString("estado_pago"));
        return data;
    }

    private boolean transicionValida(String actual, String nuevo) {
        if ("pendiente".equals(actual) && ("listo".equals(nuevo) || "cancelado".equals(nuevo))) {
            return true;
        }
        if ("listo".equals(actual) && ("entregado".equals(nuevo) || "cancelado".equals(nuevo))) {
            return true;
        }
        return actual.equals(nuevo);
    }

    private void validarOcuparRequest(OcuparMesaRequest request) {
        if (request == null || request.getIdCliente() == null || request.getIdMozo() == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
