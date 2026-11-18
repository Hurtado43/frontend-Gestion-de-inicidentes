package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.sesion.SesionUsuario;
import com.empresa.salacontrol.ui.Formato;
import com.empresa.salacontrol.ui.MapaRecurrencias;
import com.empresa.salacontrol.ui.Piezas;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tablero principal. Responde a la pregunta de la mesa de ayuda: que esta fallando, que espera tecnico
 * y como vamos con los tiempos. Usa GET /api/incidentes, /api/usuarios y /api/reportes/*.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Tablero de la mesa de ayuda")
public class TableroView extends Div {

    private final ApiCliente api;
    private final SesionUsuario sesion;

    public TableroView(ApiCliente api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
        addClassNames("sc-vista", "sc-tablero");
        cargar();
    }

    private void cargar() {
        removeAll();
        try {
            List<Dto.Incidente> incidentes = api.incidentes(Map.of());
            Map<String, Dto.Usuario> usuarios = api.usuarios().stream()
                    .collect(Collectors.toMap(Dto.Usuario::id, Function.identity()));
            Dto.Resumen resumen = api.resumen();
            List<Dto.Tiempos> tiempos = api.tiempos(30);
            Set<String> recurrentes = api.recurrentes(30, 3).stream()
                    .map(Dto.Recurrente::activoAfectado).collect(Collectors.toSet());

            add(mapa(incidentes, recurrentes));
            Div fila1 = new Div(cola(resumen), sla(tiempos));
            fila1.addClassName("sc-fila");
            Div fila2 = new Div(sinTecnico(incidentes, usuarios), movimientos(incidentes, usuarios));
            fila2.addClassName("sc-fila");
            add(fila1, fila2);
        } catch (ApiException e) {
            add(Piezas.aviso(e));
        }
    }

    private Div mapa(List<Dto.Incidente> incidentes, Set<String> recurrentes) {
        MapaRecurrencias mapa = new MapaRecurrencias(incidentes, recurrentes, 30, activo ->
                UI.getCurrent().navigate(IncidentesView.class, QueryParameters.of("activo", activo)));
        Div p = Piezas.panel("Activos que más fallan", "Últimos 30 días. Toca un activo para ver sus incidentes en el "
                + "ciclo de vida; los marcados en rojo los detecta la API como problema recurrente (3 o más fallas).", mapa);
        p.addClassName("sc-panel-mapa");
        return p;
    }

    private Div cola(Dto.Resumen r) {
        Div barra = new Div();
        barra.addClassName("sc-barra-estados");
        Div lista = new Div();
        lista.addClassName("sc-lista-estados");
        for (String e : Formato.ESTADOS) {
            long n = r.porEstado().getOrDefault(e, 0L);
            if (n > 0) {
                Span tramo = new Span();
                tramo.addClassNames("tramo", Formato.claseEstado(e));
                tramo.getStyle().set("flex-grow", String.valueOf(n));
                tramo.setTitle(Formato.estado(e) + ": " + n);
                barra.add(tramo);
            }
            Div linea = new Div(Piezas.estado(e), new Span(String.valueOf(n)));
            linea.addClassName("linea");
            lista.add(linea);
        }
        Div cifras = new Div(cifra(r.total(), "incidentes registrados"), cifra(r.activos(), "por atender"),
                cifra(r.slaIncumplidos(), "con SLA incumplido"));
        cifras.addClassName("sc-cifras");
        return Piezas.panel("Estado de la cola", "Cuántos incidentes hay en cada paso del ciclo de vida.", cifras, barra, lista);
    }

    private static Div cifra(long valor, String texto) {
        Span v = new Span(String.valueOf(valor));
        v.addClassName("valor");
        Div d = new Div(v, new Span(texto));
        d.addClassName("cifra");
        return d;
    }

    private Div sla(List<Dto.Tiempos> tiempos) {
        Div filas = new Div();
        filas.addClassName("sc-semaforo");
        for (String p : Formato.PRIORIDADES) {
            tiempos.stream().filter(t -> t.prioridad().equals(p)).findFirst().ifPresent(t -> {
                Div medidor = new Div();
                medidor.addClassName("medidor");
                Span lleno = new Span();
                lleno.getStyle().set("width", t.porcentajeCumplimientoSla() + "%");
                lleno.addClassName(t.porcentajeCumplimientoSla() >= 90 ? "bien"
                        : t.porcentajeCumplimientoSla() >= 70 ? "atento" : "mal");
                medidor.add(lleno);
                Span pct = new Span(String.format("%.0f%%", t.porcentajeCumplimientoSla()));
                pct.addClassName("pct");
                Span detalle = new Span("Meta " + t.slaHorasResolucion() + " h, promedio real "
                        + Formato.minutos(t.promedioMinutosResolucion()));
                detalle.addClassName("detalle");
                Div fila = new Div(Piezas.prioridad(p), medidor, pct, detalle);
                fila.addClassName("fila");
                filas.add(fila);
            });
        }
        return Piezas.panel("Cumplimiento del SLA", "Porcentaje de incidentes atendidos dentro del plazo de su prioridad "
                + "(últimos 30 días).", filas);
    }

    private Div sinTecnico(List<Dto.Incidente> incidentes, Map<String, Dto.Usuario> usuarios) {
        List<Dto.Incidente> abiertos = incidentes.stream().filter(i -> "ABIERTO".equals(i.estado()))
                .sorted(Comparator.comparing((Dto.Incidente i) -> Formato.PRIORIDADES.indexOf(i.prioridad()))
                        .thenComparing(Dto.Incidente::fechaCreacion))
                .toList();
        Div lista = new Div();
        lista.addClassName("sc-lista");
        if (abiertos.isEmpty()) lista.add(Piezas.vacio("Ningún incidente espera técnico."));
        for (Dto.Incidente i : abiertos) {
            Anchor titulo = new Anchor(RouteConfiguration.forSessionScope().getUrl(DetalleIncidenteView.class, i.id()),
                    i.codigo() + "  " + i.titulo());
            titulo.addClassName("titulo");
            Button asignar = new Button("Asignar automáticamente", e -> {
                try {
                    Dto.Incidente r = api.asignarAutomatico(i.id(), sesion.id());
                    Piezas.ok(r.codigo() + " asignado a " + Piezas.nombre(usuarios, r.tecnicoAsignadoId()));
                    cargar();
                } catch (ApiException ex) {
                    Piezas.error(ex);
                }
            });
            asignar.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Div meta = new Div(Piezas.prioridad(i.prioridad()), new Span(Formato.categoria(i.categoria())),
                    Piezas.sla(i));
            meta.addClassName("meta");
            Div item = new Div(new Div(titulo, meta), asignar);
            item.addClassName("item");
            lista.add(item);
        }
        return Piezas.panel("Esperando técnico", "La asignación automática elige al técnico con menos carga y "
                + "prefiere al especialista de la categoría.", lista);
    }

    private Div movimientos(List<Dto.Incidente> incidentes, Map<String, Dto.Usuario> usuarios) {
        record Mov(Dto.Incidente inc, Dto.Evento ev) { }
        List<Mov> movs = incidentes.stream()
                .flatMap(i -> i.historial().stream().map(e -> new Mov(i, e)))
                .filter(m -> m.ev().fecha() != null)
                .sorted(Comparator.comparing((Mov m) -> m.ev().fecha()).reversed())
                .limit(8).toList();
        Div lista = new Div();
        lista.addClassName("sc-linea-tiempo");
        for (Mov m : movs) {
            Span cuando = new Span(Formato.fechaHora(m.ev().fecha()));
            cuando.addClassName("cuando");
            String que = Formato.tipoEvento(m.ev().tipo())
                    + ("CAMBIO_ESTADO".equals(m.ev().tipo()) ? " a " + Formato.estado(m.ev().valorNuevo()) : "");
            Anchor inc = new Anchor(RouteConfiguration.forSessionScope().getUrl(DetalleIncidenteView.class, m.inc().id()),
                    m.inc().codigo());
            Div texto = new Div(inc, new Span(" " + que + " por " + Piezas.nombre(usuarios, m.ev().usuarioId())));
            Div punto = new Div(cuando, texto);
            punto.addClassName("evento");
            lista.add(punto);
        }
        return Piezas.panel("Últimos movimientos", "Lo más reciente del historial de todos los incidentes.", lista);
    }
}
