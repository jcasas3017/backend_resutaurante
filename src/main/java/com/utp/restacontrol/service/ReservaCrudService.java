package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.reserva.ReservaCreateRequest;
import com.utp.restacontrol.dto.reserva.ReservaDto;
import com.utp.restacontrol.dto.reserva.ReservaSituacionRequest;
import com.utp.restacontrol.dto.reserva.ReservaUpdateRequest;
import com.utp.restacontrol.model.Cliente;
import com.utp.restacontrol.model.EstadoReserva;
import com.utp.restacontrol.model.Mesa;
import com.utp.restacontrol.model.Reserva;
import com.utp.restacontrol.repository.ClienteRepository;
import com.utp.restacontrol.repository.MesaRepository;
import com.utp.restacontrol.repository.ReservaRepository;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservaCrudService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final MesaRepository mesaRepository;

    public ReservaCrudService(
            ReservaRepository reservaRepository,
            ClienteRepository clienteRepository,
            MesaRepository mesaRepository
    ) {
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.mesaRepository = mesaRepository;
    }

    public Page<ReservaDto> listar(
            String search,
            String estado,
            UUID idMesa,
            UUID idCliente,
            int page,
            int size
    ) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "fechaHora"));

        String estadoFiltro = estado == null || estado.isBlank() ? null : normalizeEstado(estado);
        return reservaRepository.buscarReservas(search, estadoFiltro, idMesa, idCliente, pageable)
                .map(this::toDto);
    }

    public ReservaDto obtener(UUID id) {
        return toDto(requireReserva(id));
    }

    public ReservaDto crear(ReservaCreateRequest request) {
        validarCrear(request);

        Reserva reserva = new Reserva();
        reserva.setTipo(request.getTipo().trim());
        reserva.setIdCliente(request.getIdCliente());
        reserva.setNombreContacto(request.getNombreContacto().trim());
        reserva.setIdMesa(request.getIdMesa());
        reserva.setFechaHora(request.getFechaHora());
        reserva.setCantidadPersonas(request.getCantidadPersonas());
        reserva.setEstado(resolveEstado(request.getEstado()));
        reserva.setObservacion(trimToNull(request.getObservacion()));
        reserva.setConfirmada(request.getConfirmada() != null ? request.getConfirmada() : false);

        Reserva saved = reservaRepository.save(reserva);
        return toDto(requireReserva(saved.getId()));
    }

    public ReservaDto actualizar(UUID id, ReservaUpdateRequest request) {
        validarActualizar(id, request);

        Reserva reserva = requireReserva(id);
        reserva.setTipo(request.getTipo().trim());
        reserva.setIdCliente(request.getIdCliente());
        reserva.setNombreContacto(request.getNombreContacto().trim());
        reserva.setIdMesa(request.getIdMesa());
        reserva.setFechaHora(request.getFechaHora());
        reserva.setCantidadPersonas(request.getCantidadPersonas());
        reserva.setEstado(resolveEstado(request.getEstado()));
        reserva.setObservacion(trimToNull(request.getObservacion()));
        reserva.setConfirmada(request.getConfirmada() != null ? request.getConfirmada() : false);

        Reserva saved = reservaRepository.save(reserva);
        return toDto(requireReserva(saved.getId()));
    }

    public ReservaDto cambiarEstado(UUID id, boolean confirmada) {
        Reserva reserva = requireReserva(id);
        reserva.setConfirmada(confirmada);
        Reserva saved = reservaRepository.save(reserva);
        return toDto(requireReserva(saved.getId()));
    }

    public ReservaDto cambiarSituacion(UUID id, ReservaSituacionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        Reserva reserva = requireReserva(id);

        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            reserva.setEstado(resolveEstado(request.getEstado()));
        }
        if (request.getConfirmada() != null) {
            reserva.setConfirmada(request.getConfirmada());
        }

        Reserva saved = reservaRepository.save(reserva);
        return toDto(requireReserva(saved.getId()));
    }

    public void eliminarLogico(UUID id) {
        Reserva reserva = requireReserva(id);
        reserva.setEstado(EstadoReserva.cancelada.name());
        reserva.setConfirmada(false);
        reservaRepository.save(reserva);
    }

    public byte[] exportarExcel(String search, String estado, UUID idMesa, UUID idCliente) {
        String estadoFiltro = estado == null || estado.isBlank() ? null : normalizeEstado(estado);
        List<Reserva> reservas = reservaRepository.exportarReservas(search, estadoFiltro, idMesa, idCliente);

        Map<UUID, String> clientesPorId = mapearClientes(reservas);
        Map<UUID, String> mesasPorId = mapearMesas(reservas);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reservas");
            crearEncabezado(sheet, workbook);
            escribirFilas(sheet, reservas, clientesPorId, mesasPorId);
            autoSize(sheet, 11);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", ex);
        }
    }

    private Reserva requireReserva(UUID id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }

    private void validarCrear(ReservaCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        validarCamposBase(
                request.getTipo(),
                request.getIdCliente(),
                request.getNombreContacto(),
                request.getIdMesa(),
                request.getFechaHora(),
                request.getCantidadPersonas(),
                request.getEstado()
        );
    }

    private void validarActualizar(UUID id, ReservaUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Body requerido");
        }
        requireReserva(id);
        validarCamposBase(
                request.getTipo(),
                request.getIdCliente(),
                request.getNombreContacto(),
                request.getIdMesa(),
                request.getFechaHora(),
                request.getCantidadPersonas(),
                request.getEstado()
        );
    }

    private void validarCamposBase(
            String tipo,
            UUID idCliente,
            String nombreContacto,
            UUID idMesa,
            LocalDateTime fechaHora,
            Integer cantidadPersonas,
                String estado
    ) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo es obligatorio");
        }
        if (idCliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (clienteRepository.findById(idCliente).isEmpty()) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        if (nombreContacto == null || nombreContacto.isBlank()) {
            throw new IllegalArgumentException("El nombre de contacto es obligatorio");
        }
        if (idMesa == null) {
            throw new IllegalArgumentException("La mesa es obligatoria");
        }
        if (mesaRepository.findById(idMesa).isEmpty()) {
            throw new IllegalArgumentException("Mesa no encontrada");
        }
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora es obligatoria");
        }
        if (cantidadPersonas == null || cantidadPersonas < 1) {
            throw new IllegalArgumentException("La cantidad de personas debe ser mayor a 0");
        }
        resolveEstado(estado);
    }

    private String resolveEstado(String estado) {
        String resolved = estado == null || estado.isBlank() ? EstadoReserva.pendiente.name() : normalizeEstado(estado);

        for (EstadoReserva value : EstadoReserva.values()) {
            if (value.name().equalsIgnoreCase(resolved)) {
                return value.name();
            }
        }
        throw new IllegalArgumentException("Estado de reserva no valido. Usa: pendiente, confirmada, cancelada, atendida");
    }

    private String normalizeEstado(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReservaDto toDto(Reserva reserva) {
        ReservaDto dto = new ReservaDto();
        dto.setId(reserva.getId());
        dto.setCodigo(reserva.getCodigo());
        dto.setTipo(reserva.getTipo());
        dto.setIdCliente(reserva.getIdCliente());
        dto.setNombreContacto(reserva.getNombreContacto());
        dto.setIdMesa(reserva.getIdMesa());
        dto.setFechaHora(reserva.getFechaHora());
        dto.setCantidadPersonas(reserva.getCantidadPersonas());
        dto.setEstado(reserva.getEstado());
        dto.setObservacion(reserva.getObservacion());
        dto.setConfirmada(reserva.getConfirmada());
        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setFechaActualizacion(reserva.getFechaActualizacion());
        return dto;
    }

    private void crearEncabezado(Sheet sheet, Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);

        Row row = sheet.createRow(0);
        String[] headers = {
                "Codigo", "Tipo", "Cliente", "Contacto", "Mesa", "Fecha Hora",
                "Personas", "Estado", "Confirmada", "Observacion", "Creacion", "Actualizacion"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

        private void escribirFilas(
            Sheet sheet,
            List<Reserva> reservas,
            Map<UUID, String> clientesPorId,
            Map<UUID, String> mesasPorId
        ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (int i = 0; i < reservas.size(); i++) {
            Reserva reserva = reservas.get(i);
            Row row = sheet.createRow(i + 1);

            setCell(row, 0, reserva.getCodigo());
            setCell(row, 1, reserva.getTipo());
            setCell(row, 2, clientesPorId.getOrDefault(reserva.getIdCliente(), ""));
            setCell(row, 3, reserva.getNombreContacto());
            setCell(row, 4, mesasPorId.getOrDefault(reserva.getIdMesa(), ""));
            setCell(row, 5, reserva.getFechaHora() != null ? reserva.getFechaHora().format(formatter) : "");
            setCell(row, 6, reserva.getCantidadPersonas());
            setCell(row, 7, reserva.getEstado());
            setCell(row, 8, reserva.getConfirmada() != null && reserva.getConfirmada() ? "Si" : "No");
            setCell(row, 9, reserva.getObservacion());
            setCell(row, 10, reserva.getFechaCreacion() != null ? reserva.getFechaCreacion().format(formatter) : "");
            setCell(row, 11, reserva.getFechaActualizacion() != null ? reserva.getFechaActualizacion().format(formatter) : "");
        }
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void setCell(Row row, int index, String value) {
        row.createCell(index).setCellValue(value == null ? "" : value);
    }

    private void setCell(Row row, int index, Integer value) {
        if (value == null) {
            row.createCell(index).setCellValue("");
        } else {
            row.createCell(index).setCellValue(value);
        }
    }

    private Map<UUID, String> mapearClientes(List<Reserva> reservas) {
        Set<UUID> idsCliente = reservas.stream()
                .map(Reserva::getIdCliente)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (idsCliente.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> nombres = new HashMap<>();
        List<Cliente> clientes = clienteRepository.findAllById(idsCliente);
        for (Cliente cliente : clientes) {
            String nombreCompleto = (safe(cliente.getNombres()) + " " + safe(cliente.getApellidos())).trim();
            nombres.put(cliente.getId(), nombreCompleto);
        }
        return nombres;
    }

    private Map<UUID, String> mapearMesas(List<Reserva> reservas) {
        Set<UUID> idsMesa = reservas.stream()
                .map(Reserva::getIdMesa)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (idsMesa.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> mesas = new HashMap<>();
        List<Mesa> mesasDb = mesaRepository.findAllById(idsMesa);
        for (Mesa mesa : mesasDb) {
            mesas.put(mesa.getId(), safe(mesa.getCodigo()));
        }
        return mesas;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
