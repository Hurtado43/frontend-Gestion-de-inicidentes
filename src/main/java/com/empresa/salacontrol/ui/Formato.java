package com.empresa.salacontrol.ui;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Textos y formatos en espanol para los valores que llegan de la API. */
public final class Formato {

    public static final ZoneId ZONA = ZoneId.systemDefault();
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", new Locale("es")).withZone(ZONA);
    private static final DateTimeFormatter FECHA_CORTA =
            DateTimeFormatter.ofPattern("d MMM", new Locale("es")).withZone(ZONA);

    /** Orden del ciclo de vida (columnas del tablero). */
    public static final List<String> ESTADOS =
            List.of("ABIERTO", "ASIGNADO", "EN_PROGRESO", "EN_ESPERA", "RESUELTO", "CERRADO", "CANCELADO");
    public static final List<String> PRIORIDADES = List.of("CRITICA", "ALTA", "MEDIA", "BAJA");
    public static final List<String> CATEGORIAS =
            List.of("HARDWARE", "SOFTWARE", "RED", "CORREO", "SEGURIDAD", "ACCESOS", "IMPRESION", "OTRO");

    private Formato() {
    }

    public static String estado(String e) {
        if (e == null) return "";
        return switch (e) {
            case "ABIERTO" -> "Abierto";
            case "ASIGNADO" -> "Asignado";
            case "EN_PROGRESO" -> "En progreso";
            case "EN_ESPERA" -> "En espera";
            case "RESUELTO" -> "Resuelto";
            case "CERRADO" -> "Cerrado";
            case "CANCELADO" -> "Cancelado";
            default -> e;
        };
    }

    public static String prioridad(String p) {
        if (p == null) return "";
        return switch (p) {
            case "CRITICA" -> "Crítica";
            case "ALTA" -> "Alta";
            case "MEDIA" -> "Media";
            case "BAJA" -> "Baja";
            default -> p;
        };
    }

    public static String categoria(String c) {
        if (c == null) return "";
        return switch (c) {
            case "HARDWARE" -> "Hardware";
            case "SOFTWARE" -> "Software";
            case "RED" -> "Red";
            case "CORREO" -> "Correo";
            case "SEGURIDAD" -> "Seguridad";
            case "ACCESOS" -> "Accesos";
            case "IMPRESION" -> "Impresión";
            default -> "Otro";
        };
    }

    public static String rol(String r) {
        if (r == null) return "";
        return switch (r) {
            case "REPORTANTE" -> "empleado";
            case "TECNICO" -> "técnico";
            case "ADMINISTRADOR" -> "administrador";
            default -> r;
        };
    }

    public static String tipoEvento(String t) {
        if (t == null) return "";
        return switch (t) {
            case "CREACION" -> "Reportado";
            case "ASIGNACION" -> "Asignación";
            case "CAMBIO_ESTADO" -> "Cambio de estado";
            case "CAMBIO_PRIORIDAD" -> "Cambio de prioridad";
            case "COMENTARIO" -> "Comentario";
            case "RESOLUCION" -> "Solución registrada";
            case "ACTUALIZACION" -> "Datos actualizados";
            default -> t;
        };
    }

    public static String fechaHora(Instant i) {
        return i == null ? "—" : FECHA_HORA.format(i);
    }

    public static String fechaCorta(Instant i) {
        return i == null ? "—" : FECHA_CORTA.format(i);
    }

    /** 75 -> "1 h 15 min", 3000 -> "2 d 2 h". */
    public static String minutos(Number minutos) {
        if (minutos == null) return "—";
        long m = Math.round(minutos.doubleValue());
        if (m < 60) return m + " min";
        long h = m / 60;
        if (h < 24) return h + " h" + (m % 60 == 0 ? "" : " " + (m % 60) + " min");
        return (h / 24) + " d" + (h % 24 == 0 ? "" : " " + (h % 24) + " h");
    }

    /** Tiempo que falta (o que paso) respecto a la fecha limite de SLA. */
    public static String sla(Instant limite, boolean incumplido, boolean terminado) {
        if (limite == null) return "";
        if (terminado) return incumplido ? "SLA incumplido" : "Dentro del SLA";
        long min = Duration.between(Instant.now(), limite).toMinutes();
        return min >= 0 ? "Vence en " + minutos(min) : "Vencido hace " + minutos(-min);
    }

    public static String claseEstado(String e) {
        return "estado-" + (e == null ? "" : e.toLowerCase().replace('_', '-'));
    }

    public static String clasePrioridad(String p) {
        return "prioridad-" + (p == null ? "" : p.toLowerCase());
    }
}
