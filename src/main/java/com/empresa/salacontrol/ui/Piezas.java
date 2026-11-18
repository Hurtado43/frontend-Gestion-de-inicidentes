package com.empresa.salacontrol.ui;

import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.util.Map;

/** Piezas visuales reutilizables de la sala de control. */
public final class Piezas {

    private Piezas() {
    }

    public static Span estado(String estado) {
        Span s = new Span(Formato.estado(estado));
        s.addClassNames("sc-estado", Formato.claseEstado(estado));
        return s;
    }

    public static Span prioridad(String prioridad) {
        Span s = new Span(Formato.prioridad(prioridad));
        s.addClassNames("sc-prioridad", Formato.clasePrioridad(prioridad));
        return s;
    }

    public static Span sla(Dto.Incidente i) {
        boolean terminado = i.fechaResolucion() != null || "CANCELADO".equals(i.estado()) || "CERRADO".equals(i.estado());
        Span s = new Span(Formato.sla(i.fechaLimiteSla(), i.slaIncumplido(), terminado));
        s.addClassName("sc-sla");
        if (i.slaIncumplido()) s.addClassName("vencido");
        else if (!terminado && i.fechaLimiteSla() != null
                && i.fechaLimiteSla().minusSeconds(3600).isBefore(java.time.Instant.now())) s.addClassName("por-vencer");
        return s;
    }

    public static Div panel(String titulo, String ayuda, Component... contenido) {
        Div d = new Div();
        d.addClassName("sc-panel");
        if (titulo != null) d.add(new H3(titulo));
        if (ayuda != null) {
            Paragraph p = new Paragraph(ayuda);
            p.addClassName("sc-ayuda");
            d.add(p);
        }
        d.add(contenido);
        return d;
    }

    public static Div aviso(ApiException e) {
        Div d = new Div(e.mensajeCompleto());
        d.addClassName("sc-aviso");
        return d;
    }

    public static Div vacio(String texto) {
        Div d = new Div(texto);
        d.addClassName("sc-vacio");
        return d;
    }

    public static void ok(String texto) {
        Notification n = Notification.show(texto, 3500, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public static void error(ApiException e) {
        String prefijo = switch (e.getStatus()) {
            case 0 -> "";
            case 400 -> "Datos inválidos. ";
            case 404 -> "No encontrado. ";
            case 409 -> "La API rechazó la operación. ";
            default -> "";
        };
        Notification n = Notification.show(prefijo + e.mensajeCompleto(), 7000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /** Nombre de un usuario a partir de su id (las respuestas de la API traen solo ids). */
    public static String nombre(Map<String, Dto.Usuario> usuarios, String id) {
        if (id == null) return "Sin asignar";
        Dto.Usuario u = usuarios.get(id);
        return u == null ? "Usuario " + id.substring(0, Math.min(6, id.length())) : u.nombre();
    }
}
