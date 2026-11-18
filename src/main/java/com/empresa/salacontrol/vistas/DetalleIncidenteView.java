package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.sesion.SesionUsuario;
import com.empresa.salacontrol.ui.Acciones;
import com.empresa.salacontrol.ui.Formato;
import com.empresa.salacontrol.ui.Piezas;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ficha de un incidente: datos, siguiente paso del ciclo de vida, asignacion, prioridad, comentarios,
 * linea de tiempo del historial y antecedentes del mismo activo (incidentes ya resueltos).
 */
@Route(value = "incidente", layout = MainLayout.class)
@PageTitle("Ficha del incidente")
public class DetalleIncidenteView extends Div implements HasUrlParameter<String> {

    private final ApiCliente api;
    private final SesionUsuario sesion;
    private String id;
    private Map<String, Dto.Usuario> usuarios = Map.of();

    public DetalleIncidenteView(ApiCliente api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
        addClassNames("sc-vista", "sc-detalle");
    }

    @Override
    public void setParameter(BeforeEvent event, String parametro) {
        this.id = parametro;
        cargar();
    }

    private void cargar() {
        removeAll();
        try {
            Dto.Incidente i = api.incidente(id);
            usuarios = api.usuarios().stream().collect(Collectors.toMap(Dto.Usuario::id, Function.identity()));
            add(cabecera(i));
            Div columnas = new Div(new Div(datos(i), lineaDeTiempo(i)), new Div(pasos(i), gestion(i), comentar(i), antecedentes(i)));
            columnas.addClassName("sc-detalle-columnas");
            add(columnas);
        } catch (ApiException e) {
            add(Piezas.aviso(e));
        }
    }

    private Div cabecera(Dto.Incidente i) {
        Anchor volver = new Anchor(RouteConfiguration.forSessionScope().getUrl(IncidentesView.class), "Volver al ciclo de vida");
        volver.addClassName("volver");
        Span codigo = new Span(i.codigo());
        codigo.addClassName("codigo");
        H2 titulo = new H2(i.titulo());
        Div etiquetas = new Div(Piezas.estado(i.estado()), Piezas.prioridad(i.prioridad()), Piezas.sla(i));
        etiquetas.addClassName("etiquetas");
        Div d = new Div(volver, codigo, titulo, etiquetas);
        d.addClassName("sc-detalle-cabecera");
        return d;
    }

    private Div datos(Dto.Incidente i) {
        Div tabla = new Div();
        tabla.addClassName("sc-datos");
        fila(tabla, "Descripción", i.descripcion());
        fila(tabla, "Categoría", Formato.categoria(i.categoria()));
        fila(tabla, "Activo afectado", i.activoAfectado() == null ? "—" : i.activoAfectado());
        fila(tabla, "Reportado por", Piezas.nombre(usuarios, i.reportadoPorId()));
        fila(tabla, "Técnico", Piezas.nombre(usuarios, i.tecnicoAsignadoId()));
        fila(tabla, "Reportado", Formato.fechaHora(i.fechaCreacion()));
        fila(tabla, "Fecha límite (SLA)", Formato.fechaHora(i.fechaLimiteSla()));
        fila(tabla, "Primera respuesta", Formato.minutos(i.minutosPrimeraRespuesta()));
        fila(tabla, "Tiempo de resolución", Formato.minutos(i.minutosResolucion()));
        if (i.solucion() != null) fila(tabla, "Solución", i.solucion());
        return Piezas.panel("Datos del incidente", null, tabla);
    }

    private static void fila(Div tabla, String etiqueta, String valor) {
        Span e = new Span(etiqueta);
        e.addClassName("etiqueta");
        Span v = new Span(valor == null ? "—" : valor);
        v.addClassName("valor");
        tabla.add(e, v);
    }

    private Div pasos(Dto.Incidente i) {
        Div botones = new Div();
        botones.addClassName("sc-pasos");
        if (i.siguientesEstados().isEmpty()) {
            botones.add(Piezas.vacio("Estado final: el incidente ya no admite cambios."));
        }
        for (String destino : i.siguientesEstados()) {
            Button b = new Button(textoPaso(i.estado(), destino), e -> Acciones.mover(api, sesion, i, destino, r -> cargar()));
            if ("CANCELADO".equals(destino)) b.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            else b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            botones.add(b);
        }
        return Piezas.panel("Siguiente paso", "Solo aparecen los cambios de estado que permite el ciclo de vida.", botones);
    }

    private static String textoPaso(String desde, String destino) {
        return switch (destino) {
            case "ASIGNADO" -> "Asignar técnico";
            case "EN_PROGRESO" -> "RESUELTO".equals(desde) ? "Reabrir" : "EN_ESPERA".equals(desde) ? "Retomar" : "Iniciar atención";
            case "EN_ESPERA" -> "Poner en espera";
            case "RESUELTO" -> "Registrar solución";
            case "CERRADO" -> "Cerrar";
            case "CANCELADO" -> "Cancelar incidente";
            default -> Formato.estado(destino);
        };
    }

    private Div gestion(Dto.Incidente i) {
        boolean final_ = i.siguientesEstados().isEmpty();
        Button reasignar = new Button(i.tecnicoAsignadoId() == null ? "Asignar técnico" : "Reasignar",
                e -> Acciones.elegirTecnico(api, sesion, i, r -> cargar()));
        reasignar.setEnabled(!final_);
        Select<String> prioridad = new Select<>();
        prioridad.setLabel("Prioridad");
        prioridad.setItems(Formato.PRIORIDADES);
        prioridad.setItemLabelGenerator(Formato::prioridad);
        prioridad.setValue(i.prioridad());
        prioridad.setEnabled(!final_);
        prioridad.addValueChangeListener(e -> {
            if (!e.isFromClient() || e.getValue().equals(i.prioridad())) return;
            try {
                api.cambiarPrioridad(i.id(), e.getValue(), sesion.id());
                Piezas.ok("Prioridad cambiada a " + Formato.prioridad(e.getValue()) + "; el SLA se recalculó");
                cargar();
            } catch (ApiException ex) {
                Piezas.error(ex);
            }
        });
        Div d = new Div(prioridad, reasignar);
        d.addClassName("sc-gestion");
        return Piezas.panel("Prioridad y técnico", "Cambiar la prioridad recalcula la fecha límite del SLA.", d);
    }

    private Div comentar(Dto.Incidente i) {
        TextArea texto = new TextArea();
        texto.setPlaceholder("Escribe un avance o una nota para el equipo");
        texto.setWidthFull();
        Button enviar = new Button("Agregar comentario", e -> {
            if (texto.getValue().isBlank()) return;
            try {
                api.comentar(i.id(), sesion.id(), texto.getValue());
                Piezas.ok("Comentario agregado a " + i.codigo());
                cargar();
            } catch (ApiException ex) {
                Piezas.error(ex);
            }
        });
        Div lista = new Div();
        lista.addClassName("sc-comentarios");
        i.comentarios().stream().sorted(Comparator.comparing(Dto.Comentario::fecha).reversed()).forEach(c -> {
            Span quien = new Span(Piezas.nombre(usuarios, c.autorId()) + ", " + Formato.fechaHora(c.fecha()));
            quien.addClassName("quien");
            Div item = new Div(quien, new Paragraph(c.texto()));
            item.addClassName("comentario");
            lista.add(item);
        });
        return Piezas.panel("Comentarios", null, texto, enviar, lista);
    }

    private Div lineaDeTiempo(Dto.Incidente i) {
        Div lista = new Div();
        lista.addClassName("sc-linea-tiempo");
        List<Dto.Evento> eventos = i.historial().stream().sorted(Comparator.comparing(Dto.Evento::fecha)).toList();
        for (Dto.Evento ev : eventos) {
            Span cuando = new Span(Formato.fechaHora(ev.fecha()));
            cuando.addClassName("cuando");
            String que = Formato.tipoEvento(ev.tipo());
            if ("CAMBIO_ESTADO".equals(ev.tipo())) que += ": " + Formato.estado(ev.valorAnterior()) + " a " + Formato.estado(ev.valorNuevo());
            if ("CAMBIO_PRIORIDAD".equals(ev.tipo())) que += ": " + Formato.prioridad(ev.valorAnterior()) + " a " + Formato.prioridad(ev.valorNuevo());
            if ("ASIGNACION".equals(ev.tipo())) que += ": " + Piezas.nombre(usuarios, ev.valorNuevo());
            Span quien = new Span(Piezas.nombre(usuarios, ev.usuarioId()));
            quien.addClassName("quien");
            Div texto = new Div(new Span(que));
            if (ev.descripcion() != null && !"ASIGNACION".equals(ev.tipo()) && !"CREACION".equals(ev.tipo())) {
                Paragraph detalle = new Paragraph(ev.descripcion());
                detalle.addClassName("detalle");
                texto.add(detalle);
            }
            Div punto = new Div(cuando, texto, quien);
            punto.addClassNames("evento", "tipo-" + ev.tipo().toLowerCase().replace('_', '-'));
            lista.add(punto);
        }
        return Piezas.panel("Historial", "Cada cambio queda registrado con fecha y responsable.", lista);
    }

    private Div antecedentes(Dto.Incidente i) {
        Div lista = new Div();
        lista.addClassName("sc-lista");
        try {
            List<Dto.Incidente> previos = api.antecedentes(i.id());
            if (previos.isEmpty()) lista.add(Piezas.vacio("No hay incidentes resueltos antes sobre este activo."));
            for (Dto.Incidente p : previos) {
                Anchor a = new Anchor(RouteConfiguration.forSessionScope().getUrl(DetalleIncidenteView.class, p.id()),
                        p.codigo() + "  " + p.titulo());
                a.addClassName("titulo");
                Paragraph sol = new Paragraph("Solución: " + p.solucion());
                sol.addClassName("solucion");
                Div item = new Div(a, sol);
                item.addClassName("item-col");
                lista.add(item);
            }
        } catch (ApiException e) {
            lista.add(Piezas.aviso(e));
        }
        return Piezas.panel("Antecedentes del activo", "Soluciones aplicadas antes en " +
                (i.activoAfectado() == null ? "este activo" : i.activoAfectado()) + ".", lista);
    }
}
