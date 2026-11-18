package com.empresa.salacontrol.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Representacion en el frontend de los JSON que devuelve la API REST.
 * Son espejos de las clases del modelo del backend (ver diagrama de clases).
 */
public final class Dto {

    private Dto() {
    }

    public record Usuario(String id, String nombre, String email, String rol, String departamento,
                          String especialidad, boolean activo, Instant fechaRegistro) {
        public boolean esTecnico() {
            return "TECNICO".equals(rol);
        }
    }

    public record Evento(Instant fecha, String tipo, String usuarioId, String descripcion,
                         String valorAnterior, String valorNuevo) {
    }

    public record Comentario(String autorId, String texto, Instant fecha) {
    }

    public record Incidente(String id, String codigo, String titulo, String descripcion, String categoria,
                            String prioridad, String estado, String activoAfectado, String reportadoPorId,
                            String tecnicoAsignadoId, Instant fechaCreacion, Instant fechaAsignacion,
                            Instant fechaPrimeraRespuesta, Instant fechaResolucion, Instant fechaCierre,
                            Instant fechaLimiteSla, String solucion, Long minutosPrimeraRespuesta,
                            Long minutosResolucion, boolean slaIncumplido, List<String> siguientesEstados,
                            List<Comentario> comentarios, List<Evento> historial) {
    }

    public record Resumen(long total, long activos, long slaIncumplidos, Map<String, Long> porEstado,
                          Map<String, Long> porPrioridad, Map<String, Long> porCategoria) {
    }

    public record Recurrente(String categoria, String activoAfectado, long cantidad, Instant ultimaOcurrencia,
                             List<String> codigos) {
    }

    public record Tiempos(String prioridad, long total, long resueltos, Double promedioMinutosRespuesta,
                          Double promedioMinutosResolucion, int slaHorasResolucion, long slaIncumplidos,
                          double porcentajeCumplimientoSla) {
    }

    public record Desempeno(String tecnicoId, String nombre, long asignados, long activos, long resueltos,
                            Double promedioMinutosResolucion, long slaIncumplidos) {
    }

    public record Error(int status, String error, String mensaje, String ruta, List<String> detalles) {
    }
}
