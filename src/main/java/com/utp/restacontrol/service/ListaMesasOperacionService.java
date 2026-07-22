package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.operacion.AgregarItemRequest;
import com.utp.restacontrol.dto.operacion.AtencionSituacionRequest;
import com.utp.restacontrol.dto.operacion.CambiarEstadoItemRequest;
import com.utp.restacontrol.dto.operacion.CobrarAtencionRequest;
import com.utp.restacontrol.dto.operacion.OcuparMesaRequest;
import com.utp.restacontrol.dto.operacion.OperacionItemRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ListaMesasOperacionService {

    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
        private static final DateTimeFormatter REPORT_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

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
                    item.put("estadoOperativo",
                            calcularEstadoOperativo(activa, situacion, idAtencion != null, idReserva != null));
                    item.put("reservaActiva", buildReservaActiva(rs, idReserva));
                    item.put("atencionActiva", buildAtencionActiva(rs, idAtencion));
                    item.put("totalActual", rs.getDouble("total_actual"));
                    Timestamp reservaTs = rs.getTimestamp("reserva_fecha_hora");
                    Map<String, Object> margen = calcularMargen(idReserva,
                            reservaTs != null ? reservaTs.toLocalDateTime() : null, ref);
                    item.put("bloqueadaPorMargen", margen.get("bloqueadaPorMargen"));
                    item.put("proximaOcupacionEn", margen.get("proximaOcupacionEn"));
                    item.put("minutosParaOcupacion", margen.get("minutosParaOcupacion"));
                    item.put("motivoBloqueo", margen.get("motivoBloqueo"));
                    return item;
                },
                Timestamp.valueOf(ref),
                Timestamp.valueOf(ref));

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
                        "situacion", rs.getString("situacion")) : null,
                idMesa);

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
                        "confirmada", rs.getBoolean("confirmada")) : null,
                idMesa,
                Timestamp.valueOf(ref),
                Timestamp.valueOf(ref));

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
                        "estadoPago", rs.getString("estado_pago")) : null,
                idMesa);

        Map<String, Object> pedidoActual = null;
        if (atencionActiva != null) {
            UUID idAtencion = (UUID) atencionActiva.get("idAtencion");
            pedidoActual = construirPedidoActual(idAtencion);
        }

        boolean activa = Boolean.TRUE.equals(mesa.get("activa"));
        String situacion = text((String) mesa.get("situacion"));
        String estadoOperativo = calcularEstadoOperativo(activa, situacion, atencionActiva != null,
                reservaActiva != null);

        LocalDateTime reservaFechaHoraCtx = reservaActiva != null ? (LocalDateTime) reservaActiva.get("fechaHora")
                : null;
        UUID idReservaCtx = reservaActiva != null ? (UUID) reservaActiva.get("idReserva") : null;
        Map<String, Object> margenCtx = calcularMargen(idReservaCtx, reservaFechaHoraCtx, ref);

        Map<String, Object> result = new HashMap<>();
        result.put("mesa", mesa);
        result.put("estadoOperativo", estadoOperativo);
        result.put("bloqueadaPorMargen", margenCtx.get("bloqueadaPorMargen"));
        result.put("proximaOcupacionEn", margenCtx.get("proximaOcupacionEn"));
        result.put("minutosParaOcupacion", margenCtx.get("minutosParaOcupacion"));
        result.put("motivoBloqueo", margenCtx.get("motivoBloqueo"));
        result.put("reservaActiva", reservaActiva);
        result.put("atencionActiva", atencionActiva);
        result.put("pedidoActual", pedidoActual);
        return result;
    }

    public Map<String, Object> obtenerPedidoActual(UUID idAtencion) {
        return construirPedidoActual(idAtencion);
    }

    public Map<String, Object> obtenerReporteVentas(String periodo) {
        String rango = periodo == null ? "7d" : periodo.trim().toLowerCase(Locale.ROOT);
        int dias = switch (rango) {
            case "1d" -> 1;
            case "7d" -> 7;
            case "30d" -> 30;
            default -> 7;
        };

        LocalDateTime fin = LocalDateTime.now(LIMA_ZONE).withHour(23).withMinute(59).withSecond(59).withNano(0);
        LocalDateTime inicio = fin.minusDays(dias - 1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Map<String, Object>> history = jdbcTemplate.query(
                """
                        SELECT DATE(a.cierre_en AT TIME ZONE 'America/Lima')::date AS fecha,
                               COUNT(*) FILTER (WHERE a.estado::text = 'Cerrada' OR a.estado::text = 'cerrada') AS atenciones,
                               COALESCE(SUM(COALESCE(a.total_pagado, 0)), 0) AS ventas,
                               COALESCE(AVG(COALESCE(a.total_pagado, 0)), 0) AS ticket_promedio
                        FROM atenciones a
                        WHERE a.cierre_en IS NOT NULL
                          AND a.cierre_en >= ?::timestamp
                          AND a.cierre_en <= ?::timestamp
                        GROUP BY DATE(a.cierre_en AT TIME ZONE 'America/Lima')
                        ORDER BY fecha ASC
                        """,
                (rs, rowNum) -> {
                    double ventas = rs.getDouble("ventas");
                    int atenciones = rs.getInt("atenciones");
                    double ticketPromedio = atenciones > 0 ? ventas / atenciones : 0d;
                    return Map.<String, Object>of(
                            "label", rs.getDate("fecha").toLocalDate().getDayOfWeek().name().substring(0, 3),
                            "ventas", roundCurrency(ventas),
                            "ocupacion", 0,
                            "atenciones", atenciones,
                            "ticketPromedio", roundCurrency(ticketPromedio));
                },
                Timestamp.valueOf(inicio),
                Timestamp.valueOf(fin));

        double ventasDia = history.stream().mapToDouble(item -> ((Number) item.get("ventas")).doubleValue()).sum();
        int atencionesDia = history.stream().mapToInt(item -> ((Number) item.get("atenciones")).intValue()).sum();
        double ticketPromedio = atencionesDia > 0 ? ventasDia / atencionesDia : 0d;

        Map<String, Object> mejorDia = history.stream()
                .max((left, right) -> Double.compare(((Number) left.get("ventas")).doubleValue(), ((Number) right.get("ventas")).doubleValue()))
                .orElse(Map.of("label", "Sin datos", "ventas", 0, "ocupacion", 0));

        int ocupacionMedia = history.isEmpty() ? 0 : (int) Math.round(
                history.stream()
                        .mapToDouble(item -> ((Number) item.get("ocupacion")).doubleValue())
                        .average()
                        .orElse(0d));

        Map<String, Object> data = new HashMap<>();
        data.put("ventasDia", roundCurrency(ventasDia));
        data.put("ticketPromedio", roundCurrency(ticketPromedio));
        data.put("ocupacionMedia", ocupacionMedia);
        data.put("mejorDia", Map.of(
                "label", mejorDia.getOrDefault("label", "Sin datos"),
                "ventas", roundCurrency(((Number) mejorDia.getOrDefault("ventas", 0)).doubleValue()),
                "ocupacion", mejorDia.getOrDefault("ocupacion", 0)));
        data.put("history", history);

        return Map.of("success", true, "data", data);
    }

    public Map<String, Object> obtenerReporteReservas(String periodo) {
        String rango = periodo == null ? "7d" : periodo.trim().toLowerCase(Locale.ROOT);
        int dias = switch (rango) {
            case "1d" -> 1;
            case "7d" -> 7;
            case "30d" -> 30;
            default -> 7;
        };

        LocalDateTime fin = LocalDateTime.now(LIMA_ZONE).withHour(23).withMinute(59).withSecond(59).withNano(0);
        LocalDateTime inicio = fin.minusDays(dias - 1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        Map<String, Object> totales = jdbcTemplate.query(
                """
                        SELECT COUNT(*) AS total_reservas,
                               COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'confirmada') AS confirmadas,
                               COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'pendiente') AS pendientes,
                               COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'cancelada') AS canceladas,
                               COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'atendida') AS atendidas
                        FROM reservas
                        WHERE fecha_hora >= ?::timestamp
                          AND fecha_hora <= ?::timestamp
                        """,
                rs -> {
                    if (!rs.next()) {
                        return Map.of(
                                "totalReservas", 0,
                                "confirmadas", 0,
                                "pendientes", 0,
                                "canceladas", 0,
                                "atencionesEnCurso", 0,
                                "avgTiempoMin", 0);
                    }
                    return Map.of(
                            "totalReservas", rs.getInt("total_reservas"),
                            "confirmadas", rs.getInt("confirmadas"),
                            "pendientes", rs.getInt("pendientes"),
                            "canceladas", rs.getInt("canceladas"),
                            "atencionesEnCurso", 0,
                            "avgTiempoMin", 0);
                },
                Timestamp.valueOf(inicio),
                Timestamp.valueOf(fin));

        Integer atencionesEnCurso = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM atenciones
                        WHERE LOWER(COALESCE(estado::text, '')) IN ('en_curso', 'en curso')
                        """,
                Integer.class);

        Double avgTiempoMin = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (cierre_en - apertura_en)) / 60), 0)
                        FROM atenciones
                        WHERE cierre_en IS NOT NULL
                          AND apertura_en IS NOT NULL
                        """,
                Double.class);

        List<Map<String, Object>> history = new ArrayList<>();
        LocalDate cursor = inicio.toLocalDate();
        LocalDate endDate = fin.toLocalDate();
        while (!cursor.isAfter(endDate)) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay().minusNanos(1);
            Map<String, Object> dayTotals = jdbcTemplate.query(
                    """
                            SELECT COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'confirmada') AS confirmadas,
                                   COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'pendiente') AS pendientes,
                                   COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'cancelada') AS canceladas,
                                   COUNT(*) FILTER (WHERE LOWER(COALESCE(estado::text, '')) = 'atendida') AS atendidas
                            FROM reservas
                            WHERE fecha_hora >= ?::timestamp
                              AND fecha_hora < ?::timestamp
                            """,
                    rs -> {
                        if (!rs.next()) {
                            return Map.of("confirmadas", 0, "pendientes", 0, "canceladas", 0, "atendidas", 0);
                        }
                        return Map.of(
                                "confirmadas", rs.getInt("confirmadas"),
                                "pendientes", rs.getInt("pendientes"),
                                "canceladas", rs.getInt("canceladas"),
                                "atendidas", rs.getInt("atendidas"));
                    },
                    Timestamp.valueOf(dayStart),
                    Timestamp.valueOf(dayEnd));

            Integer atenciones = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(1)
                            FROM atenciones
                            WHERE apertura_en >= ?::timestamp
                              AND apertura_en < ?::timestamp
                            """,
                    Integer.class,
                    Timestamp.valueOf(dayStart),
                    Timestamp.valueOf(dayEnd));

            Map<String, Object> item = new HashMap<>();
            item.put("label", cursor.getDayOfWeek().name().substring(0, 3) + " " + cursor.getDayOfMonth());
            item.put("confirmadas", dayTotals.get("confirmadas"));
            item.put("pendientes", dayTotals.get("pendientes"));
            item.put("canceladas", dayTotals.get("canceladas"));
            item.put("atenciones", atenciones == null ? 0 : atenciones);
            history.add(item);
            cursor = cursor.plusDays(1);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totales", Map.of(
                "totalReservas", totales.get("totalReservas"),
                "confirmadas", totales.get("confirmadas"),
                "pendientes", totales.get("pendientes"),
                "canceladas", totales.get("canceladas"),
                "atencionesEnCurso", atencionesEnCurso == null ? 0 : atencionesEnCurso,
                "avgTiempoMin", roundCurrency(avgTiempoMin == null ? 0d : avgTiempoMin)));
        data.put("history", history);
        return Map.of("success", true, "data", data);
    }

        public Map<String, Object> obtenerReportePlatosConsumidos(LocalDate fechaInicio, LocalDate fechaFin) {
                DateRange range = resolveDateRange(fechaInicio, fechaFin);

                List<Map<String, Object>> rawItems = jdbcTemplate.query(
                                """
                                                SELECT dp.id_plato AS id_plato,
                                                           COALESCE(pl.nombre, 'Plato') AS nombre,
                                                           COALESCE(c.nombre, 'Sin categoria') AS categoria,
                                                           COALESCE(SUM(dp.cantidad), 0) AS cantidad,
                                                           COALESCE(SUM((dp.cantidad * dp.precio_unit) - COALESCE(dp.descuento, 0)), 0) AS monto
                                                FROM atenciones a
                                                INNER JOIN pedidos p ON p.id_atencion = a.id
                                                INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                                                LEFT JOIN platos pl ON pl.id = dp.id_plato
                                                LEFT JOIN categorias c ON c.id = pl.id_categoria
                                                WHERE a.cierre_en IS NOT NULL
                                                  AND a.cierre_en >= ?::timestamp
                                                  AND a.cierre_en <= ?::timestamp
                                                  AND LOWER(COALESCE(a.estado::text, '')) = 'cerrada'
                                                  AND dp.id_plato IS NOT NULL
                                                  AND LOWER(COALESCE(dp.estado_cocina::text, '')) <> 'cancelado'
                                                GROUP BY dp.id_plato, pl.nombre, c.nombre
                                                ORDER BY cantidad DESC, monto DESC, nombre ASC
                                                """,
                                (rs, rowNum) -> {
                                        Map<String, Object> item = new HashMap<>();
                                        item.put("rank", rowNum + 1);
                                        item.put("idPlato", rs.getObject("id_plato"));
                                        item.put("nombre", rs.getString("nombre"));
                                        item.put("categoria", rs.getString("categoria"));
                                        item.put("cantidad", rs.getInt("cantidad"));
                                        item.put("monto", roundCurrency(rs.getDouble("monto")));
                                        return item;
                                },
                                Timestamp.valueOf(range.startDateTime()),
                                Timestamp.valueOf(range.endDateTime()));

                double totalPorciones = rawItems.stream()
                                .mapToDouble(item -> ((Number) item.get("cantidad")).doubleValue())
                                .sum();
                double totalVentasPlatos = rawItems.stream()
                                .mapToDouble(item -> ((Number) item.get("monto")).doubleValue())
                                .sum();

                List<Map<String, Object>> items = new ArrayList<>();
                for (Map<String, Object> item : rawItems) {
                        Map<String, Object> mapped = new HashMap<>(item);
                        double cantidad = ((Number) item.get("cantidad")).doubleValue();
                        mapped.put("participacionPct", roundCurrency(totalPorciones > 0 ? (cantidad * 100d / totalPorciones) : 0d));
                        items.add(mapped);
                }

                Map<String, Object> platoTop = new HashMap<>();
                if (!items.isEmpty()) {
                        Map<String, Object> top = items.get(0);
                        platoTop.put("idPlato", top.get("idPlato"));
                        platoTop.put("nombre", top.get("nombre"));
                        platoTop.put("categoria", top.get("categoria"));
                        platoTop.put("cantidad", top.get("cantidad"));
                        platoTop.put("monto", top.get("monto"));
                } else {
                        platoTop.put("idPlato", null);
                        platoTop.put("nombre", "Sin datos");
                        platoTop.put("categoria", "Sin categoria");
                        platoTop.put("cantidad", 0);
                        platoTop.put("monto", 0d);
                }

                List<Map<String, Object>> trendRows = jdbcTemplate.query(
                                """
                                                SELECT DATE(a.cierre_en AT TIME ZONE 'America/Lima')::date AS fecha,
                                                           COALESCE(SUM(dp.cantidad), 0) AS cantidad,
                                                           COALESCE(SUM((dp.cantidad * dp.precio_unit) - COALESCE(dp.descuento, 0)), 0) AS monto
                                                FROM atenciones a
                                                INNER JOIN pedidos p ON p.id_atencion = a.id
                                                INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                                                WHERE a.cierre_en IS NOT NULL
                                                  AND a.cierre_en >= ?::timestamp
                                                  AND a.cierre_en <= ?::timestamp
                                                  AND LOWER(COALESCE(a.estado::text, '')) = 'cerrada'
                                                  AND dp.id_plato IS NOT NULL
                                                  AND LOWER(COALESCE(dp.estado_cocina::text, '')) <> 'cancelado'
                                                GROUP BY DATE(a.cierre_en AT TIME ZONE 'America/Lima')
                                                ORDER BY fecha ASC
                                                """,
                                (rs, rowNum) -> {
                                        Map<String, Object> item = new HashMap<>();
                                        LocalDate fecha = rs.getDate("fecha").toLocalDate();
                                        item.put("fecha", fecha);
                                        item.put("cantidad", rs.getInt("cantidad"));
                                        item.put("monto", roundCurrency(rs.getDouble("monto")));
                                        return item;
                                },
                                Timestamp.valueOf(range.startDateTime()),
                                Timestamp.valueOf(range.endDateTime()));

                Map<LocalDate, Map<String, Object>> trendMap = new LinkedHashMap<>();
                LocalDate cursor = range.startDate();
                while (!cursor.isAfter(range.endDate())) {
                        Map<String, Object> day = new HashMap<>();
                        day.put("fecha", cursor.toString());
                        day.put("label", cursor.format(REPORT_LABEL_FORMATTER));
                        day.put("cantidad", 0);
                        day.put("monto", 0d);
                        trendMap.put(cursor, day);
                        cursor = cursor.plusDays(1);
                }

                for (Map<String, Object> row : trendRows) {
                        LocalDate fecha = (LocalDate) row.get("fecha");
                        Map<String, Object> day = trendMap.get(fecha);
                        if (day == null) {
                                continue;
                        }
                        day.put("cantidad", row.get("cantidad"));
                        day.put("monto", row.get("monto"));
                }

                Map<String, Object> resumen = new HashMap<>();
                resumen.put("totalPorciones", (int) Math.round(totalPorciones));
                resumen.put("totalVentasPlatos", roundCurrency(totalVentasPlatos));
                resumen.put("platoTop", platoTop);

                Map<String, Object> data = new HashMap<>();
                data.put("resumen", resumen);
                data.put("items", items);
                data.put("trend", new ArrayList<>(trendMap.values()));
                return Map.of("success", true, "data", data);
        }

        public Map<String, Object> obtenerReporteCaja(LocalDate fechaInicio, LocalDate fechaFin) {
                DateRange range = resolveDateRange(fechaInicio, fechaFin);

                Map<String, Object> totales = jdbcTemplate.query(
                                """
                                                WITH atenciones_cerradas AS (
                                                        SELECT a.id,
                                                                   COALESCE(a.propina, 0) AS propina
                                                        FROM atenciones a
                                                        WHERE a.cierre_en IS NOT NULL
                                                          AND a.cierre_en >= ?::timestamp
                                                          AND a.cierre_en <= ?::timestamp
                                                          AND LOWER(COALESCE(a.estado::text, '')) = 'cerrada'
                                                ),
                                                items_por_atencion AS (
                                                        SELECT p.id_atencion,
                                                                   COALESCE(SUM(dp.cantidad * dp.precio_unit), 0) AS bruto,
                                                                   COALESCE(SUM(COALESCE(dp.descuento, 0)), 0) AS descuentos
                                                        FROM pedidos p
                                                        INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                                                        WHERE LOWER(COALESCE(dp.estado_cocina::text, '')) <> 'cancelado'
                                                        GROUP BY p.id_atencion
                                                )
                                                SELECT COALESCE(SUM(COALESCE(i.bruto, 0)), 0) AS bruto,
                                                           COALESCE(SUM(COALESCE(i.descuentos, 0)), 0) AS descuentos,
                                                           COALESCE(SUM(ac.propina), 0) AS propinas,
                                                           COUNT(*) AS tickets
                                                FROM atenciones_cerradas ac
                                                LEFT JOIN items_por_atencion i ON i.id_atencion = ac.id
                                                """,
                                rs -> {
                                        if (!rs.next()) {
                                                return Map.of("bruto", 0d, "descuentos", 0d, "propinas", 0d, "tickets", 0);
                                        }
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("bruto", roundCurrency(rs.getDouble("bruto")));
                                        data.put("descuentos", roundCurrency(rs.getDouble("descuentos")));
                                        data.put("propinas", roundCurrency(rs.getDouble("propinas")));
                                        data.put("tickets", rs.getInt("tickets"));
                                        return data;
                                },
                                Timestamp.valueOf(range.startDateTime()),
                                Timestamp.valueOf(range.endDateTime()));

                int anulados = jdbcTemplate.queryForObject(
                                """
                                                SELECT COUNT(1)
                                                FROM atenciones a
                                                WHERE a.cierre_en IS NOT NULL
                                                  AND a.cierre_en >= ?::timestamp
                                                  AND a.cierre_en <= ?::timestamp
                                                  AND LOWER(COALESCE(a.estado::text, '')) = 'cancelada'
                                                """,
                                Integer.class,
                                Timestamp.valueOf(range.startDateTime()),
                                Timestamp.valueOf(range.endDateTime()));

                double ingresosBrutos = ((Number) totales.get("bruto")).doubleValue();
                double descuentos = ((Number) totales.get("descuentos")).doubleValue();
                double propinas = ((Number) totales.get("propinas")).doubleValue();
                int tickets = ((Number) totales.get("tickets")).intValue();
                double netoVentas = roundCurrency(ingresosBrutos - descuentos);
                double montoCaja = roundCurrency(netoVentas + propinas);
                double ticketPromedio = roundCurrency(tickets > 0 ? netoVentas / tickets : 0d);

                List<Map<String, Object>> metodosPagoRaw = jdbcTemplate.query(
                                """
                                                SELECT COALESCE(NULLIF(TRIM(c.metodo_pago), ''), 'Sin definir') AS metodo,
                                                           COALESCE(SUM(COALESCE(c.monto_total, 0)), 0) AS monto,
                                                           COUNT(*) AS tickets
                                                FROM comprobantes c
                                                WHERE c.fecha_emision IS NOT NULL
                                                  AND c.fecha_emision >= ?::timestamp
                                                  AND c.fecha_emision <= ?::timestamp
                                                  AND LOWER(COALESCE(c.estado::text, '')) NOT IN ('anulado', 'cancelado')
                                                GROUP BY COALESCE(NULLIF(TRIM(c.metodo_pago), ''), 'Sin definir')
                                                ORDER BY monto DESC, metodo ASC
                                                """,
                                (rs, rowNum) -> {
                                        Map<String, Object> method = new HashMap<>();
                                        method.put("metodo", rs.getString("metodo"));
                                        method.put("monto", roundCurrency(rs.getDouble("monto")));
                                        method.put("tickets", rs.getInt("tickets"));
                                        return method;
                                },
                                Timestamp.valueOf(range.startDateTime()),
                                Timestamp.valueOf(range.endDateTime()));

                double totalMetodos = metodosPagoRaw.stream()
                                .mapToDouble(item -> ((Number) item.get("monto")).doubleValue())
                                .sum();

                List<Map<String, Object>> metodosPago = new ArrayList<>();
                for (Map<String, Object> method : metodosPagoRaw) {
                        Map<String, Object> mapped = new HashMap<>(method);
                        double monto = ((Number) method.get("monto")).doubleValue();
                        mapped.put("porcentaje", roundCurrency(totalMetodos > 0 ? (monto * 100d / totalMetodos) : 0d));
                        metodosPago.add(mapped);
                }

                List<Map<String, Object>> historyRows = jdbcTemplate.query(
                                """
                                                WITH atenciones_cerradas AS (
                                                        SELECT a.id,
                                                                   DATE(a.cierre_en AT TIME ZONE 'America/Lima')::date AS fecha,
                                                                   COALESCE(a.propina, 0) AS propina
                                                        FROM atenciones a
                                                        WHERE a.cierre_en IS NOT NULL
                                                          AND a.cierre_en >= ?::timestamp
                                                          AND a.cierre_en <= ?::timestamp
                                                          AND LOWER(COALESCE(a.estado::text, '')) = 'cerrada'
                                                ),
                                                items_por_atencion AS (
                                                        SELECT p.id_atencion,
                                                                   COALESCE(SUM(dp.cantidad * dp.precio_unit), 0) AS bruto,
                                                                   COALESCE(SUM(COALESCE(dp.descuento, 0)), 0) AS descuentos
                                                        FROM pedidos p
                                                        INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                                                        WHERE LOWER(COALESCE(dp.estado_cocina::text, '')) <> 'cancelado'
                                                        GROUP BY p.id_atencion
                                                )
                                                SELECT ac.fecha,
                                                           COALESCE(SUM(COALESCE(i.bruto, 0)), 0) AS bruto,
                                                           COALESCE(SUM(COALESCE(i.descuentos, 0)), 0) AS descuentos,
                                                           COALESCE(SUM(ac.propina), 0) AS propinas,
                                                           COUNT(*) AS tickets
                                                FROM atenciones_cerradas ac
                                                LEFT JOIN items_por_atencion i ON i.id_atencion = ac.id
                                                GROUP BY ac.fecha
                                                ORDER BY ac.fecha ASC
                                                """,
                                (rs, rowNum) -> {
                                        Map<String, Object> day = new HashMap<>();
                                        day.put("fecha", rs.getDate("fecha").toLocalDate());
                                        day.put("bruto", roundCurrency(rs.getDouble("bruto")));
                                        day.put("descuentos", roundCurrency(rs.getDouble("descuentos")));
                                        day.put("propinas", roundCurrency(rs.getDouble("propinas")));
                                        day.put("tickets", rs.getInt("tickets"));
                                        return day;
                                },
                                Timestamp.valueOf(range.startDateTime()),
                                Timestamp.valueOf(range.endDateTime()));

                Map<LocalDate, Map<String, Object>> historyMap = new LinkedHashMap<>();
                LocalDate cursor = range.startDate();
                while (!cursor.isAfter(range.endDate())) {
                        Map<String, Object> day = new HashMap<>();
                        day.put("fecha", cursor.toString());
                        day.put("label", cursor.format(REPORT_LABEL_FORMATTER));
                        day.put("bruto", 0d);
                        day.put("descuentos", 0d);
                        day.put("propinas", 0d);
                        day.put("neto", 0d);
                        day.put("caja", 0d);
                        day.put("tickets", 0);
                        historyMap.put(cursor, day);
                        cursor = cursor.plusDays(1);
                }

                for (Map<String, Object> row : historyRows) {
                        LocalDate fecha = (LocalDate) row.get("fecha");
                        Map<String, Object> day = historyMap.get(fecha);
                        if (day == null) {
                                continue;
                        }

                        double bruto = ((Number) row.get("bruto")).doubleValue();
                        double dayDescuentos = ((Number) row.get("descuentos")).doubleValue();
                        double dayPropinas = ((Number) row.get("propinas")).doubleValue();
                        double dayNeto = roundCurrency(bruto - dayDescuentos);
                        double dayCaja = roundCurrency(dayNeto + dayPropinas);

                        day.put("bruto", roundCurrency(bruto));
                        day.put("descuentos", roundCurrency(dayDescuentos));
                        day.put("propinas", roundCurrency(dayPropinas));
                        day.put("neto", dayNeto);
                        day.put("caja", dayCaja);
                        day.put("tickets", row.get("tickets"));
                }

                Map<String, Object> resumen = new HashMap<>();
                resumen.put("ingresosBrutos", roundCurrency(ingresosBrutos));
                resumen.put("descuentos", roundCurrency(descuentos));
                resumen.put("propinas", roundCurrency(propinas));
                resumen.put("netoVentas", netoVentas);
                resumen.put("montoCaja", montoCaja);
                resumen.put("tickets", tickets);
                resumen.put("ticketPromedio", ticketPromedio);
                resumen.put("anulados", anulados);

                Map<String, Object> data = new HashMap<>();
                data.put("resumen", resumen);
                data.put("metodosPago", metodosPago);
                data.put("history", new ArrayList<>(historyMap.values()));
                return Map.of("success", true, "data", data);
        }

    public Map<String, Object> obtenerDashboardMetrics() {
        Integer categoriasActivas = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM categorias WHERE activo = true",
                Integer.class);
        Integer categoriasTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM categorias",
                Integer.class);

        Integer platosDisponibles = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM platos WHERE activo = true",
                Integer.class);
        Integer platosTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM platos",
                Integer.class);

        Integer mesasActivas = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM mesas WHERE activa = true",
                Integer.class);
        Integer mesasTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM mesas",
                Integer.class);

        Integer reservasVigentes = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM reservas WHERE estado::text IN ('pendiente', 'confirmada')",
                Integer.class);
        Integer reservasConfirmadas = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM reservas WHERE estado::text = 'confirmada'",
                Integer.class);

        Integer atencionesEnCurso = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM atenciones WHERE estado::text = 'en_curso'",
                Integer.class);
        Integer atencionesCerradasHoy = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM atenciones WHERE estado::text = 'cerrada' AND fecha_creacion::date = CURRENT_DATE",
                Integer.class);

        Integer pedidosTotales = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pedidos",
                Integer.class);
        Integer itemsTotales = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM detalle_pedidos",
                Integer.class);

        List<Map<String, Object>> reservas = jdbcTemplate.query(
                """
                        SELECT c.nombres || ' ' || c.apellidos AS cliente,
                               m.codigo AS mesa,
                               to_char(r.fecha_hora, 'DD/MM/YYYY, HH24:MI') AS fecha_hora,
                               r.cantidad_personas AS personas,
                               r.estado::text AS estado
                        FROM reservas r
                        LEFT JOIN clientes c ON c.id = r.id_cliente
                        LEFT JOIN mesas m ON m.id = r.id_mesa
                        WHERE r.estado::text IN ('pendiente', 'confirmada')
                        ORDER BY r.fecha_hora DESC
                        LIMIT 5
                        """,
                (rs, rowNum) -> Map.of(
                        "cliente", rs.getString("cliente"),
                        "mesa", rs.getString("mesa"),
                        "fechaHora", rs.getString("fecha_hora"),
                        "personas", rs.getInt("personas"),
                        "estado", rs.getString("estado")
                ));

        List<Map<String, Object>> atenciones = jdbcTemplate.query(
                """
                        SELECT c.nombres || ' ' || c.apellidos AS cliente,
                               m.codigo AS mesa,
                               u.nombres || ' ' || u.apellidos AS mozo,
                               a.estado::text AS estado,
                               a.estado_pago::text AS pago
                        FROM atenciones a
                        LEFT JOIN clientes c ON c.id = a.id_cliente
                        LEFT JOIN mesas m ON m.id = a.id_mesa
                        LEFT JOIN usuarios u ON u.id = a.id_mozo
                        WHERE a.estado::text = 'en_curso'
                        ORDER BY a.apertura_en DESC
                        LIMIT 5
                        """,
                (rs, rowNum) -> Map.of(
                        "cliente", rs.getString("cliente"),
                        "mesa", rs.getString("mesa"),
                        "mozo", rs.getString("mozo"),
                        "estado", rs.getString("estado"),
                        "pago", rs.getString("pago")
                ));

        Map<String, Object> data = new HashMap<>();
        data.put("categorias", Map.of("active", categoriasActivas, "total", categoriasTotal));
        data.put("platos", Map.of("available", platosDisponibles, "total", platosTotal));
        data.put("mesas", Map.of("active", mesasActivas, "total", mesasTotal));
        data.put("reservas", Map.of("active", reservasVigentes, "confirmed", reservasConfirmadas, "rows", reservas));
        data.put("atenciones", Map.of("inProgress", atencionesEnCurso, "closedToday", atencionesCerradasHoy, "rows", atenciones));
        data.put("pedidos", Map.of("total", pedidosTotales, "items", itemsTotales));
        return Map.of("data", data);
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
                        "situacion", rs.getString("situacion")) : null,
                idMesa);

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
                idMesa);

        if (abiertas != null && abiertas > 0) {
            throw new OperacionBusinessException("Mesa ya ocupada", "MESA_OCUPADA");
        }

        LocalDateTime ahora = LocalDateTime.now(LIMA_ZONE);
        Integer reservaEnMargen;
        if (idReserva == null) {
            reservaEnMargen = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(1)
                            FROM reservas
                            WHERE id_mesa = ?::uuid
                              AND estado::text IN ('pendiente', 'confirmada')
                              AND fecha_hora BETWEEN (?::timestamp - INTERVAL '1 hour') AND (?::timestamp + INTERVAL '30 minutes')
                            """,
                    Integer.class,
                    idMesa,
                    Timestamp.valueOf(ahora),
                    Timestamp.valueOf(ahora));
        } else {
            reservaEnMargen = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(1)
                            FROM reservas
                            WHERE id_mesa = ?::uuid
                              AND estado::text IN ('pendiente', 'confirmada')
                              AND fecha_hora BETWEEN (?::timestamp - INTERVAL '1 hour') AND (?::timestamp + INTERVAL '30 minutes')
                              AND id != ?::uuid
                            """,
                    Integer.class,
                    idMesa,
                    Timestamp.valueOf(ahora),
                    Timestamp.valueOf(ahora),
                    idReserva);
        }
        if (reservaEnMargen != null && reservaEnMargen > 0) {
            throw new OperacionBusinessException("Mesa bloqueada por margen de reserva (30 min antes / 1 hora despues)",
                    "MESA_BLOQUEADA_MARGEN");
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
                    idMesa);
            if (vigente == null || vigente == 0) {
                throw new OperacionBusinessException("Reserva no vigente", "RESERVA_NO_VIGENTE");
            }
        }

        UUID idAtencion = insertarAtencion(idMesa, idReserva, request);
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            UUID idPedido = insertarPedido(idAtencion, request.getNotas(), request.getIdMozo());
            for (OperacionItemRequest item : request.getItems()) {
                insertarDetalleItem(idPedido, item, request.getIdMozo());
            }
        }

        if (idReserva != null) {
            jdbcTemplate.update(
                    """
                            UPDATE reservas
                            SET estado = 'confirmada',
                                confirmada = true
                            WHERE id = ?::uuid
                            """,
                    idReserva);
        }

        return contexto(idMesa, LocalDateTime.now(LIMA_ZONE));
    }

    @Transactional
    public Map<String, Object> agregarItem(UUID idAtencion, AgregarItemRequest request) {
        if (request == null || request.getTipoItem() == null || request.getTipoItem().isBlank()
                || request.getIdItem() == null || request.getCantidad() == null || request.getCantidad() < 1) {
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
                idAtencion);

        if (idPedido == null) {
            idPedido = insertarPedido(idAtencion, null, null);
        }

        UUID idDetalle = insertarDetalleItem(idPedido, toOperacionItem(request), null);

        return Map.of(
                "idDetalle", idDetalle,
                "idPedido", idPedido);
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
                        "estado", rs.getString("estado_cocina")) : null,
                idDetalle);

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
                        SET estado_cocina = ?
                        WHERE id = ?::uuid
                        """,
                nuevo,
                idDetalle);

        return Map.of(
                "idDetalle", idDetalle,
                "estadoCocina", nuevo);
    }

    @Transactional
    public Map<String, Object> cancelarDetalle(UUID idDetalle) {
        Map<String, Object> detalle = jdbcTemplate.query(
                """
                        SELECT id,
                               id_pedido,
                               id_producto,
                               cantidad,
                               estado_cocina::text AS estado_cocina
                        FROM detalle_pedidos
                        WHERE id = ?::uuid
                        FOR UPDATE
                        """,
                rs -> rs.next() ? Map.of(
                        "id", rs.getObject("id"),
                        "idPedido", rs.getObject("id_pedido"),
                        "idProducto", rs.getObject("id_producto"),
                        "cantidad", rs.getInt("cantidad"),
                        "estadoCocina", rs.getString("estado_cocina")) : null,
                idDetalle);

        if (detalle == null) {
            throw new OperacionBusinessException("Item no valido", "ITEM_NO_VALIDO");
        }

        String actual = text((String) detalle.get("estadoCocina"));
        if ("cancelado".equals(actual)) {
            return Map.of(
                    "idDetalle", idDetalle,
                    "estadoCocina", "cancelado");
        }

        UUID idProducto = (UUID) detalle.get("idProducto");
        int cantidad = (Integer) detalle.get("cantidad");
        if (idProducto != null && cantidad > 0) {
            jdbcTemplate.update(
                    """
                            UPDATE productos
                            SET stock = stock + ?
                            WHERE id = ?::uuid
                            """,
                    cantidad,
                    idProducto);
        }

        jdbcTemplate.update(
                """
                        UPDATE detalle_pedidos
                        SET estado_cocina = 'cancelado'
                        WHERE id = ?::uuid
                        """,
                idDetalle);

        return Map.of(
                "idDetalle", idDetalle,
                "estadoCocina", "cancelado");
    }

    @Transactional
    public Map<String, Object> cobrarAtencion(UUID idAtencion, CobrarAtencionRequest request) {
        if (request == null || request.getMetodoPago() == null || request.getMetodoPago().isBlank()) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }

        Map<String, Object> atencion = jdbcTemplate.query(
                """
                        SELECT a.id,
                               a.codigo,
                               a.id_cliente,
                               a.id_mesa,
                               a.id_reserva,
                               a.id_mozo,
                               c.nombres AS cliente_nombres,
                               c.apellidos AS cliente_apellidos,
                               c.documento AS cliente_documento,
                               a.estado::text AS estado
                        FROM atenciones a
                        INNER JOIN clientes c ON c.id = a.id_cliente
                        WHERE a.id = ?::uuid
                        FOR UPDATE
                        """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> row = new java.util.HashMap<>();
                    row.put("id", rs.getObject("id"));
                    row.put("codigo", rs.getString("codigo"));
                    row.put("idCliente", rs.getObject("id_cliente"));
                    row.put("idMesa", rs.getObject("id_mesa"));
                    row.put("idReserva", rs.getObject("id_reserva"));
                    row.put("idMozo", rs.getObject("id_mozo"));
                    row.put("clienteNombres", rs.getString("cliente_nombres"));
                    row.put("clienteApellidos", rs.getString("cliente_apellidos"));
                    row.put("clienteDocumento", rs.getString("cliente_documento"));
                    row.put("estado", rs.getString("estado"));
                    return row;
                },
                idAtencion);

        if (atencion == null) {
            throw new OperacionBusinessException("Atencion no encontrada", "ATENCION_NO_ENCONTRADA");
        }

        String estadoAtencion = text((String) atencion.get("estado"));
        if (!"en curso".equals(estadoAtencion) && !"en_curso".equals(estadoAtencion)) {
            throw new OperacionBusinessException("Atencion no en curso", "ATENCION_NO_EN_CURSO");
        }

        Double subtotal = request.getSubtotal();
        if (subtotal == null) {
            subtotal = jdbcTemplate.queryForObject(
                    """
                            SELECT COALESCE(SUM((dp.cantidad * dp.precio_unit) - COALESCE(dp.descuento, 0)), 0)
                            FROM pedidos p
                            INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                            WHERE p.id_atencion = ?::uuid
                              AND dp.estado_cocina::text <> 'cancelado'
                            """,
                    Double.class,
                    idAtencion);
        }

        double propina = request.getPropina() == null ? 0d : Math.max(request.getPropina(), 0d);
        double total = request.getTotal() == null ? (subtotal == null ? 0d : subtotal) + propina : Math.max(request.getTotal(), 0d);

        jdbcTemplate.update(
                """
                        UPDATE atenciones
                        SET estado = 'Cerrada',
                            estado_pago = 'Pagado',
                            cierre_en = now(),
                            total_pagado = ?,
                            propina = ?
                        WHERE id = ?::uuid
                        """,
                total,
                propina,
                idAtencion);

        UUID idReserva = (UUID) atencion.get("idReserva");
        if (idReserva != null) {
            jdbcTemplate.update(
                    """
                            UPDATE reservas
                            SET estado = 'Completada',
                                confirmada = true
                            WHERE id = ?::uuid
                            """,
                    idReserva);
        }

        Map<String, Object> comprobanteData = null;
        if (Boolean.TRUE.equals(request.getGenerarComprobante())) {
            UUID idMozo = (UUID) atencion.get("idMozo");
            comprobanteData = crearComprobante(idAtencion, request.getMetodoPago(), subtotal == null ? 0d : subtotal,
                    propina, total, idMozo);
        }

        String codigoAtencion = (String) atencion.get("codigo");
        String clienteNombre = buildNombreCompleto(
                (String) atencion.get("clienteNombres"),
                (String) atencion.get("clienteApellidos"));
        String clienteDocumento = (String) atencion.get("clienteDocumento");

        Map<String, Object> result = new HashMap<>();
        if (comprobanteData != null) {
            result.putAll(comprobanteData);
        }
        result.put("subtotal", subtotal == null ? 0d : subtotal);
        result.put("propina", propina);
        result.put("total", total);
        result.put("codigoAtencion", codigoAtencion);
        result.put("clienteNombre", clienteNombre);
        result.put("clienteDocumento", clienteDocumento);
        return result;
    }

    @Transactional
    public Map<String, Object> cambiarSituacionAtencion(UUID idAtencion, AtencionSituacionRequest request) {
        if (request == null || request.getEstado() == null || request.getEstado().isBlank()) {
            throw new OperacionBusinessException("Estado requerido", "VALIDACION_NEGOCIO");
        }

        String estado = text(request.getEstado());
        if (!"cancelada".equals(estado)) {
            throw new OperacionBusinessException("Solo se permite estado cancelada", "VALIDACION_NEGOCIO");
        }

        Map<String, Object> atencion = jdbcTemplate.query(
                """
                        SELECT id, id_mesa, estado::text AS estado
                        FROM atenciones
                        WHERE id = ?::uuid
                        FOR UPDATE
                        """,
                rs -> rs.next() ? Map.of(
                        "id", rs.getObject("id"),
                        "idMesa", rs.getObject("id_mesa"),
                        "estado", rs.getString("estado")) : null,
                idAtencion);

        if (atencion == null) {
            throw new OperacionBusinessException("Atencion no encontrada", "VALIDACION_NEGOCIO");
        }

        String estadoAtencion = text((String) atencion.get("estado"));
        if (!"en curso".equals(estadoAtencion) && !"en_curso".equals(estadoAtencion)) {
            throw new OperacionBusinessException("Atencion no en curso", "VALIDACION_NEGOCIO");
        }

        Integer items = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                        FROM pedidos p
                        INNER JOIN detalle_pedidos dp ON dp.id_pedido = p.id
                        WHERE p.id_atencion = ?::uuid
                        """,
                Integer.class,
                idAtencion);

        if (items != null && items > 0) {
            throw new OperacionBusinessException("No se puede anular una atencion con items", "VALIDACION_NEGOCIO");
        }

        jdbcTemplate.update(
                """
                        UPDATE atenciones
                        SET estado = 'cancelada',
                            cierre_en = now()
                        WHERE id = ?::uuid
                        """,
                idAtencion);

        UUID idMesa = (UUID) atencion.get("idMesa");
        return Map.of(
                "idAtencion", idAtencion,
                "estado", "cancelada",
                "mesa", Map.of(
                        "idMesa", idMesa,
                        "estadoOperativo", "libre"));
    }

    private double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

        private DateRange resolveDateRange(LocalDate fechaInicio, LocalDate fechaFin) {
                LocalDate start = fechaInicio;
                LocalDate end = fechaFin;

                if (start == null && end == null) {
                        end = LocalDate.now(LIMA_ZONE);
                        start = end.minusDays(6);
                } else if (start == null) {
                        start = end;
                } else if (end == null) {
                        end = start;
                }

                if (start.isAfter(end)) {
                        LocalDate swap = start;
                        start = end;
                        end = swap;
                }

                LocalDateTime startDateTime = start.atStartOfDay();
                LocalDateTime endDateTime = end.atTime(23, 59, 59);
                return new DateRange(start, end, startDateTime, endDateTime);
        }

        private record DateRange(LocalDate startDate, LocalDate endDate, LocalDateTime startDateTime,
                        LocalDateTime endDateTime) {
        }

    private Map<String, Object> crearComprobante(UUID idAtencion, String metodoPago, double subtotal, double propina,
            double total, UUID idMozo) {
        String numeroComprobante = "CMP" + System.currentTimeMillis();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO comprobantes (
                                id_atencion, numero_comprobante, tipo_comprobante,
                                monto_subtotal, monto_igv, monto_descuento, monto_total,
                                metodo_pago, estado, emitido_por, fecha_emision
                            )
                            VALUES (?::uuid, ?, 'Boleta', ?, 0, 0, ?, ?, 'Emitido', ?::uuid, now())
                            RETURNING id
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, idAtencion);
            ps.setString(2, numeroComprobante);
            ps.setDouble(3, subtotal);
            ps.setDouble(4, total);
            ps.setString(5, metodoPago);
            ps.setObject(6, idMozo);
            return ps;
        }, keyHolder);

        Object rawIdComprobante = null;
        if (keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")) {
            rawIdComprobante = keyHolder.getKeys().get("id");
        }
        if (rawIdComprobante == null) {
            rawIdComprobante = keyHolder.getKey();
        }

        UUID idComprobante;
        if (rawIdComprobante instanceof UUID uuid) {
            idComprobante = uuid;
        } else if (rawIdComprobante != null) {
            try {
                idComprobante = UUID.fromString(rawIdComprobante.toString());
            } catch (IllegalArgumentException ex) {
                throw new OperacionBusinessException("No se pudo convertir el id del comprobante", "ERROR_INTERNO");
            }
        } else {
            throw new OperacionBusinessException("No se pudo obtener el id del comprobante", "ERROR_INTERNO");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("idComprobante", idComprobante);
        response.put("numeroComprobante", numeroComprobante);
        response.put("fechaEmision", LocalDateTime.now(LIMA_ZONE));
        response.put("metodoPago", metodoPago);
        response.put("subtotal", subtotal);
        response.put("propina", propina);
        response.put("total", total);
        return response;
    }

    private UUID insertarAtencion(UUID idMesa, UUID idReserva, OcuparMesaRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO atenciones (
                                id_mesa, id_cliente, id_mozo, id_reserva,
                                estado, estado_pago, apertura_en
                            )
                            VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid,
                                    'en_curso', 'pendiente', now())
                            RETURNING id
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, idMesa);
            ps.setObject(2, request.getIdCliente());
            ps.setObject(3, request.getIdMozo());
            ps.setObject(4, idReserva);
            return ps;
        }, keyHolder);

        Object id = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : null;
        if (!(id instanceof UUID uuid)) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }
        return uuid;
    }

    private UUID insertarPedido(UUID idAtencion, String notas, UUID creadoPor) {
        UUID createdBy = creadoPor != null ? creadoPor : obtenerCreadoPorDesdeAtencion(idAtencion);
        if (createdBy == null) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO pedidos (id_atencion, creado_por, notas, creado_en)
                            VALUES (?::uuid, ?::uuid, ?, now())
                            RETURNING id
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, idAtencion);
            ps.setObject(2, createdBy);
            ps.setString(3, trimToNull(notas));
            return ps;
        }, keyHolder);

        Object id = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : null;
        if (!(id instanceof UUID uuid)) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }
        return uuid;
    }

    private UUID obtenerCreadoPorDesdeAtencion(UUID idAtencion) {
        return jdbcTemplate.query(
                """
                        SELECT a.id_mozo
                        FROM atenciones a
                        WHERE a.id = ?::uuid
                        """,
                rs -> rs.next() ? (UUID) rs.getObject("id_mozo") : null,
                idAtencion);
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
                            "activo", rs.getBoolean("activo")) : null,
                    item.getIdItem());

            if (plato == null || !Boolean.TRUE.equals(plato.get("disponible"))
                    || !Boolean.TRUE.equals(plato.get("activo"))) {
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
                            "activo", rs.getBoolean("activo")) : null,
                    item.getIdItem());

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
                    idProducto);

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
                                precio_unit, tipo_item, descuento, estado_cocina, observaciones, fecha_creacion
                            )
                            VALUES (?::uuid, ?::uuid, ?::uuid, ?,
                                    ?, ?, 0, ?, ?, now())
                            RETURNING id
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, idPedido);
            ps.setObject(2, finalIdPlato);
            ps.setObject(3, finalIdProducto);
            ps.setInt(4, item.getCantidad());
            ps.setDouble(5, precio == null ? 0d : precio);
            ps.setString(6, text(item.getTipoItem()));
            ps.setString(7, estadoCocina);
            ps.setString(8, trimToNull(item.getObservaciones()));
            return ps;
        }, keyHolder);

        Object id = keyHolder.getKeys() != null ? keyHolder.getKeys().get("id") : null;
        if (!(id instanceof UUID uuid)) {
            throw new OperacionBusinessException("Validacion de negocio", "VALIDACION_NEGOCIO");
        }
        return uuid;
    }

    private void tryInsertMovimientoStock(UUID idProducto, int cantidad, UUID idUsuario) {
        jdbcTemplate.execute((Connection connection) -> {
            Savepoint savepoint = connection.setSavepoint("movimiento_stock_optional");
            try (PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO movimientos_stock (
                                id_producto, tipo_movimiento, cantidad,
                                referencia_tipo, referencia_id, motivo, created_by, created_at
                            )
                            VALUES (?::uuid, 'salida_venta', ?,
                                    'detalle_pedido', NULL, 'Salida por venta', ?::uuid, now())
                            """)) {
                ps.setObject(1, idProducto);
                ps.setInt(2, cantidad);
                ps.setObject(3, idUsuario);
                ps.executeUpdate();
            } catch (Exception ignored) {
                connection.rollback(savepoint);
            }
            return null;
        });
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
                idAtencion);

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
                          AND dp.estado_cocina::text <> 'cancelado'
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
                            "subtotal", (cantidad * precioUnit) - descuento);
                },
                idPedido);

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

    private Map<String, Object> calcularMargen(UUID idReserva, LocalDateTime fechaReserva, LocalDateTime ref) {
        Map<String, Object> result = new HashMap<>();
        if (idReserva == null || fechaReserva == null) {
            result.put("bloqueadaPorMargen", false);
            result.put("proximaOcupacionEn", null);
            result.put("minutosParaOcupacion", null);
            result.put("motivoBloqueo", null);
            return result;
        }
        long minutos = Duration.between(ref, fechaReserva).toMinutes();
        // antes (reserva futura): 30 min | despues (reserva pasada): 60 min
        boolean bloqueada = (minutos >= 0 && minutos <= 30) || (minutos < 0 && minutos >= -60);
        result.put("bloqueadaPorMargen", bloqueada);
        result.put("proximaOcupacionEn", bloqueada ? fechaReserva : null);
        result.put("minutosParaOcupacion", bloqueada ? (int) minutos : null);
        result.put("motivoBloqueo", bloqueada ? (minutos >= 0 ? "margen_antes" : "margen_despues") : null);
        return result;
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

    private String buildNombreCompleto(String nombres, String apellidos) {
        String nombre = nombres != null ? nombres.trim() : "";
        String apellido = apellidos != null ? apellidos.trim() : "";
        if (nombre.isEmpty() && apellido.isEmpty()) {
            return "";
        }
        return (nombre + " " + apellido).trim();
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
        if (request == null) {
            throw new OperacionBusinessException("Body requerido", "VALIDACION_NEGOCIO");
        }
        if (request.getIdCliente() == null) {
            throw new OperacionBusinessException("El idCliente es obligatorio", "VALIDACION_NEGOCIO");
        }
        if (request.getIdMozo() == null) {
            throw new OperacionBusinessException("El idMozo es obligatorio", "VALIDACION_NEGOCIO");
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
