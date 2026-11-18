package com.empresa.salacontrol.ui;

import com.empresa.salacontrol.api.Dto;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Mapa de recurrencias: una fila por activo afectado y una columna por dia. Cada celda se colorea segun
 * cuantos incidentes se reportaron ese dia sobre ese activo. Los activos que la API marca como problema
 * recurrente (agregacion en MongoDB) se resaltan.
 */
public class MapaRecurrencias extends Div {

    public MapaRecurrencias(List<Dto.Incidente> incidentes, Set<String> recurrentesApi, int dias,
                            Consumer<String> alElegirActivo) {
        addClassName("sc-mapa");
        LocalDate hoy = LocalDate.now(Formato.ZONA);
        LocalDate desde = hoy.minusDays(dias - 1L);

        Map<String, List<Dto.Incidente>> porActivo = incidentes.stream()
                .filter(i -> i.activoAfectado() != null && i.fechaCreacion() != null)
                .filter(i -> !i.fechaCreacion().atZone(Formato.ZONA).toLocalDate().isBefore(desde))
                .collect(Collectors.groupingBy(Dto.Incidente::activoAfectado));

        List<Map.Entry<String, List<Dto.Incidente>>> filas = new ArrayList<>(porActivo.entrySet());
        filas.sort(Comparator.comparing((Map.Entry<String, List<Dto.Incidente>> e) -> -e.getValue().size())
                .thenComparing(Map.Entry::getKey));
        if (filas.size() > 10) filas = filas.subList(0, 10);

        Div rejilla = new Div();
        rejilla.addClassName("sc-mapa-rejilla");
        rejilla.getStyle().set("grid-template-columns", "minmax(150px, 190px) repeat(" + dias + ", minmax(10px, 1fr)) 44px");

        // encabezado de dias: solo se rotula el primer dia, cada lunes y hoy
        rejilla.add(celdaTexto("Activo", "sc-mapa-cab"));
        for (int d = 0; d < dias; d++) {
            LocalDate dia = desde.plusDays(d);
            boolean rotular = d == 0 || dia.getDayOfWeek().getValue() == 1 || dia.equals(hoy);
            Span s = new Span(rotular ? String.valueOf(dia.getDayOfMonth()) : "");
            s.addClassName("sc-mapa-dia");
            if (dia.equals(hoy)) {
                s.addClassName("hoy");
                s.setTitle("Hoy");
            }
            rejilla.add(s);
        }
        rejilla.add(celdaTexto("Total", "sc-mapa-cab"));

        for (var fila : filas) {
            String activo = fila.getKey();
            Span nombre = new Span(activo);
            nombre.addClassName("sc-mapa-activo");
            if (recurrentesApi.contains(activo)) {
                nombre.addClassName("recurrente");
                nombre.setTitle("La API lo detectó como problema recurrente");
            }
            nombre.getElement().addEventListener("click", e -> alElegirActivo.accept(activo));
            rejilla.add(nombre);

            Map<LocalDate, List<Dto.Incidente>> porDia = new LinkedHashMap<>();
            for (Dto.Incidente i : fila.getValue()) {
                porDia.computeIfAbsent(i.fechaCreacion().atZone(Formato.ZONA).toLocalDate(), k -> new ArrayList<>()).add(i);
            }
            for (int d = 0; d < dias; d++) {
                LocalDate dia = desde.plusDays(d);
                List<Dto.Incidente> delDia = porDia.getOrDefault(dia, List.of());
                Span celda = new Span();
                celda.addClassName("sc-mapa-celda");
                celda.addClassName("n" + Math.min(delDia.size(), 3));
                if (!delDia.isEmpty()) {
                    celda.setTitle(Formato.fechaCorta(delDia.get(0).fechaCreacion()) + ": "
                            + delDia.stream().map(i -> i.codigo() + " " + i.titulo()).collect(Collectors.joining(" / ")));
                }
                rejilla.add(celda);
            }
            Span total = new Span(String.valueOf(fila.getValue().size()));
            total.addClassName("sc-mapa-total");
            rejilla.add(total);
        }
        add(rejilla);

        Div leyenda = new Div(new Span("Incidentes por día:"), muestra("n1", "1"), muestra("n2", "2"), muestra("n3", "3 o más"),
                marcaRecurrente());
        leyenda.addClassName("sc-mapa-leyenda");
        add(leyenda);
    }

    private static Span celdaTexto(String texto, String clase) {
        Span s = new Span(texto);
        s.addClassName(clase);
        return s;
    }

    private static Span muestra(String clase, String texto) {
        Span c = new Span();
        c.addClassNames("sc-mapa-celda", clase);
        Span s = new Span(c, new Span(texto));
        s.addClassName("sc-mapa-muestra");
        return s;
    }

    private static Span marcaRecurrente() {
        Span m = new Span("ACTIVO");
        m.addClassNames("sc-mapa-activo", "recurrente", "mini");
        Span s = new Span(m, new Span("detectado como recurrente por la API"));
        s.addClassName("sc-mapa-muestra");
        return s;
    }
}
