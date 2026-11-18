package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.ui.Formato;
import com.empresa.salacontrol.ui.MapaRecurrencias;
import com.empresa.salacontrol.ui.Piezas;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reportes e indicadores: problemas recurrentes (agregacion de MongoDB en la API), tiempos de respuesta
 * y resolucion por prioridad y distribucion por categoria. Los parametros cambian la consulta HTTP.
 */
@Route(value = "reportes", layout = MainLayout.class)
@PageTitle("Recurrencias y tiempos de respuesta")
public class ReportesView extends Div {

    private final ApiCliente api;
    private final Select<Integer> dias = new Select<>();
    private final Select<Integer> minimo = new Select<>();
    private final Div contenido = new Div();

    public ReportesView(ApiCliente api) {
        this.api = api;
        addClassNames("sc-vista", "sc-reportes");
        dias.setLabel("Periodo");
        dias.setItems(7, 15, 30, 60, 90);
        dias.setItemLabelGenerator(d -> "Últimos " + d + " días");
        dias.setValue(30);
        minimo.setLabel("Contar como recurrente desde");
        minimo.setItems(2, 3, 4, 5);
        minimo.setItemLabelGenerator(m -> m + " incidentes");
        minimo.setValue(3);
        dias.addValueChangeListener(e -> cargar());
        minimo.addValueChangeListener(e -> cargar());
        Div parametros = new Div(dias, minimo);
        parametros.addClassName("sc-filtros");
        add(parametros, contenido);
        cargar();
    }

    private void cargar() {
        contenido.removeAll();
        try {
            int d = dias.getValue(), m = minimo.getValue();
            List<Dto.Recurrente> recurrentes = api.recurrentes(d, m);
            List<Dto.Tiempos> tiempos = api.tiempos(d);
            Dto.Resumen resumen = api.resumen();
            List<Dto.Incidente> todos = api.incidentes(Map.of());
            Set<String> marcados = recurrentes.stream().map(Dto.Recurrente::activoAfectado).collect(Collectors.toSet());

            contenido.add(tablaRecurrentes(recurrentes, d, m));
            contenido.add(Piezas.panel("Mapa de recurrencias", null,
                    new MapaRecurrencias(todos, marcados, Math.min(d, 45), activo ->
                            UI.getCurrent().navigate(IncidentesView.class, QueryParameters.of("activo", activo)))));
            Div fila = new Div(tablaTiempos(tiempos), categorias(resumen));
            fila.addClassName("sc-fila");
            contenido.add(fila);
        } catch (ApiException e) {
            contenido.add(Piezas.aviso(e));
        }
    }

    private Div tablaRecurrentes(List<Dto.Recurrente> recurrentes, int d, int m) {
        Grid<Dto.Recurrente> g = new Grid<>();
        g.addColumn(Dto.Recurrente::activoAfectado).setHeader("Activo").setAutoWidth(true);
        g.addColumn(r -> Formato.categoria(r.categoria())).setHeader("Categoría");
        g.addColumn(Dto.Recurrente::cantidad).setHeader("Incidentes").setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);
        g.addColumn(r -> Formato.fechaHora(r.ultimaOcurrencia())).setHeader("Última falla").setAutoWidth(true);
        g.addColumn(r -> String.join(", ", r.codigos())).setHeader("Códigos").setFlexGrow(2);
        g.setItems(recurrentes);
        g.setAllRowsVisible(true);
        g.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        g.addItemClickListener(e -> UI.getCurrent().navigate(IncidentesView.class,
                QueryParameters.of("activo", e.getItem().activoAfectado())));
        Div cuerpo = recurrentes.isEmpty()
                ? Piezas.vacio("Ningún activo tuvo " + m + " o más incidentes de la misma categoría en los últimos " + d + " días.")
                : new Div(g);
        return Piezas.panel("Problemas recurrentes", "Activos con " + m + " o más incidentes de la misma categoría en los "
                + "últimos " + d + " días. Son candidatos a una solución de fondo (reemplazo, mantenimiento). Toca una fila "
                + "para ver sus incidentes.", cuerpo);
    }

    private Div tablaTiempos(List<Dto.Tiempos> tiempos) {
        Grid<Dto.Tiempos> g = new Grid<>();
        g.addComponentColumn(t -> Piezas.prioridad(t.prioridad())).setHeader("Prioridad").setAutoWidth(true);
        g.addColumn(Dto.Tiempos::total).setHeader("Total");
        g.addColumn(t -> Formato.minutos(t.promedioMinutosRespuesta())).setHeader("Primera respuesta").setAutoWidth(true);
        g.addColumn(t -> Formato.minutos(t.promedioMinutosResolucion())).setHeader("Resolución").setAutoWidth(true);
        g.addColumn(t -> t.slaHorasResolucion() + " h").setHeader("Meta SLA");
        g.addComponentColumn(t -> {
            Span s = new Span(String.format("%.0f%%", t.porcentajeCumplimientoSla()));
            s.addClassName(t.porcentajeCumplimientoSla() >= 90 ? "sc-ok" : t.porcentajeCumplimientoSla() >= 70 ? "sc-atento" : "sc-mal");
            return s;
        }).setHeader("Cumplimiento");
        g.setItems(tiempos);
        g.setAllRowsVisible(true);
        g.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        return Piezas.panel("Tiempos por prioridad", "Promedios en el periodo elegido. La primera respuesta se mide "
                + "hasta que un técnico inicia la atención.", g);
    }

    private Div categorias(Dto.Resumen resumen) {
        Div barras = new Div();
        barras.addClassName("sc-barras-h");
        long max = Math.max(1, resumen.porCategoria().values().stream().mapToLong(Long::longValue).max().orElse(1));
        resumen.porCategoria().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> {
                    Span nombre = new Span(Formato.categoria(e.getKey()));
                    Div pista = new Div();
                    pista.addClassName("pista");
                    Span lleno = new Span();
                    lleno.getStyle().set("width", (100.0 * e.getValue() / max) + "%");
                    pista.add(lleno);
                    Span n = new Span(String.valueOf(e.getValue()));
                    n.addClassName("n");
                    Div fila = new Div(nombre, pista, n);
                    fila.addClassName("fila");
                    barras.add(fila);
                });
        return Piezas.panel("Incidentes por categoría", "Todos los incidentes registrados.", barras);
    }
}
