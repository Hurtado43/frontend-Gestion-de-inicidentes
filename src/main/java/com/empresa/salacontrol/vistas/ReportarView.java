package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.sesion.SesionUsuario;
import com.empresa.salacontrol.ui.Formato;
import com.empresa.salacontrol.ui.Piezas;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulario para reportar un problema (POST /api/incidentes). Mientras se escribe el activo afectado,
 * avisa si ese activo ya tuvo fallas y muestra la solucion que se aplico (historial de recurrencias).
 */
@Route(value = "reportar", layout = MainLayout.class)
@PageTitle("Reportar un incidente")
public class ReportarView extends Div {

    private final ApiCliente api;
    private final SesionUsuario sesion;

    private final TextField titulo = new TextField("¿Qué pasa?");
    private final TextArea descripcion = new TextArea("Describe el problema");
    private final RadioButtonGroup<String> categoria = new RadioButtonGroup<>("Categoría");
    private final RadioButtonGroup<String> prioridad = new RadioButtonGroup<>("Prioridad");
    private final TextField activo = new TextField("Equipo o servicio afectado");
    private final ComboBox<Dto.Usuario> reportante = new ComboBox<>("Reportado por");
    private final Div pista = new Div();

    public ReportarView(ApiCliente api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
        addClassNames("sc-vista", "sc-reportar");

        titulo.setPlaceholder("Ej. La impresora no imprime");
        titulo.setMaxLength(120);
        titulo.setRequired(true);
        descripcion.setPlaceholder("Qué intentaste, desde cuándo ocurre, a quién afecta");
        descripcion.setMinHeight("120px");
        descripcion.setRequired(true);
        categoria.setItems(Formato.CATEGORIAS);
        categoria.setItemLabelGenerator(Formato::categoria);
        categoria.setValue("HARDWARE");
        categoria.addClassName("sc-opciones");
        prioridad.setItems(Formato.PRIORIDADES);
        prioridad.setItemLabelGenerator(p -> Formato.prioridad(p) + switch (p) {
            case "CRITICA" -> ": la empresa no puede operar (SLA 4 h)";
            case "ALTA" -> ": un área completa está detenida (SLA 8 h)";
            case "MEDIA" -> ": afecta a una persona, hay alternativa (SLA 24 h)";
            default -> ": molestia menor o solicitud (SLA 72 h)";
        });
        prioridad.setValue("MEDIA");
        prioridad.addClassName("sc-opciones-vertical");
        activo.setPlaceholder("Ej. PC-0045, ROUTER-PISO3, VPN");
        activo.setHelperText("Sirve para detectar fallas que se repiten en el mismo equipo.");
        activo.setValueChangeMode(ValueChangeMode.LAZY);
        activo.addValueChangeListener(e -> revisarAntecedentes());
        reportante.setItemLabelGenerator(u -> u.nombre() + " (" + Formato.rol(u.rol()) + ")");
        pista.addClassName("sc-pista");

        try {
            List<Dto.Usuario> usuarios = api.usuarios().stream().filter(Dto.Usuario::activo).toList();
            reportante.setItems(usuarios);
            if (sesion.hay()) usuarios.stream().filter(u -> u.id().equals(sesion.id())).findFirst().ifPresent(reportante::setValue);
        } catch (ApiException e) {
            add(Piezas.aviso(e));
        }

        Button enviar = new Button("Reportar incidente", e -> enviar());
        enviar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Div formulario = new Div(titulo, descripcion, activo, pista, categoria, prioridad, reportante, enviar);
        formulario.addClassName("sc-formulario");
        Div intro = new Div(new Paragraph("Al enviar, el sistema asigna un código (INC-…), calcula la fecha límite según "
                + "la prioridad y registra el primer evento del historial."));
        intro.addClassName("sc-ayuda-linea");
        add(Piezas.panel("Nuevo incidente", null, intro, formulario));
    }

    private void revisarAntecedentes() {
        pista.removeAll();
        String valor = activo.getValue().trim().toUpperCase();
        if (valor.length() < 3) return;
        try {
            List<Dto.Incidente> previos = api.incidentes(Map.of()).stream()
                    .filter(i -> valor.equals(i.activoAfectado())).toList();
            if (previos.isEmpty()) return;
            Span aviso = new Span(valor + " ya tiene " + previos.size() + (previos.size() == 1 ? " incidente" : " incidentes")
                    + " registrados.");
            aviso.addClassName("fuerte");
            pista.add(aviso);
            previos.stream().filter(i -> i.solucion() != null).findFirst()
                    .ifPresent(i -> pista.add(new Paragraph("Última solución aplicada (" + i.codigo() + "): " + i.solucion())));
        } catch (ApiException ignorado) {
            // la pista es opcional
        }
    }

    private void enviar() {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("titulo", titulo.getValue());
        datos.put("descripcion", descripcion.getValue());
        datos.put("categoria", categoria.getValue());
        datos.put("prioridad", prioridad.getValue());
        datos.put("activoAfectado", activo.getValue());
        datos.put("reportadoPorId", reportante.getValue() == null ? null : reportante.getValue().id());
        try {
            Dto.Incidente creado = api.reportar(datos);
            Piezas.ok("Incidente " + creado.codigo() + " reportado");
            UI.getCurrent().navigate(DetalleIncidenteView.class, creado.id());
        } catch (ApiException e) {
            Piezas.error(e);
        }
    }
}
