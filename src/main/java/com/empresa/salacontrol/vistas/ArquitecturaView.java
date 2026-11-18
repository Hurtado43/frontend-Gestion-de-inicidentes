package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.RegistroHttp;
import com.empresa.salacontrol.ui.Formato;
import com.empresa.salacontrol.ui.Piezas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Vista para el profesor: la arquitectura del sistema funcionando. Muestra las capas del diagrama de despliegue
 * (navegador, frontend Vaadin, API Spring Boot, MongoDB), permite lanzar solicitudes HTTP de ejemplo y registra
 * en vivo cada solicitud y respuesta que el frontend intercambia con la API.
 */
@Route(value = "arquitectura", layout = MainLayout.class)
@PageTitle("Arquitectura en vivo")
public class ArquitecturaView extends Div {

    private record Prueba(String nombre, Supplier<Object> llamada) { }

    private final ApiCliente api;
    private final RegistroHttp registro;
    private final ObjectMapper json;
    private final Grid<RegistroHttp.Entrada> bitacora = new Grid<>();
    private final Pre solicitud = new Pre();
    private final Pre respuesta = new Pre();
    private final Span cabeceraDetalle = new Span("Elige una fila de la bitácora para ver la solicitud y la respuesta.");
    private final Div estadoApi = new Div();
    private final Div estadoMongo = new Div();
    private long ultimoNumero = -1;

    public ArquitecturaView(ApiCliente api, RegistroHttp registro, ObjectMapper json) {
        this.api = api;
        this.registro = registro;
        this.json = json.copy().enable(SerializationFeature.INDENT_OUTPUT);
        addClassNames("sc-vista", "sc-arquitectura");

        add(Piezas.panel("Cómo viaja una solicitud", "Mismas capas del diagrama de despliegue. Cada acción en la sala de "
                + "control es una solicitud HTTP con JSON del frontend a la API, que lee o escribe en MongoDB.", diagrama()));
        add(Piezas.panel("Probar una solicitud", "Envía una solicitud real a la API y mira abajo la respuesta.", consola()));
        add(Piezas.panel("Bitácora HTTP en vivo", "Las últimas 200 solicitudes que el frontend envió a la API "
                + "(se actualiza sola cada 2 segundos).", bitacora(), detalle()));
        comprobar();
    }

    private Div diagrama() {
        Div d = new Div(
                capa("Navegador", "El usuario o el profesor", "HTML generado por Vaadin, comunicación por WebSocket/XHR", null),
                flecha("Eventos de la interfaz"),
                capa("Frontend: Sala de control", "Spring Boot + Vaadin (Java 21)", "incidentes-frontend.jar, puerto 8081", null),
                flecha("HTTP/JSON (REST)"),
                capa("Backend: API REST", "Spring Boot 3.5 (Java 21)", api.getUrlBase(), estadoApi),
                flecha("Protocolo de MongoDB, TCP 27017"),
                capa("Base de datos", "MongoDB", "usuarios, incidentes, contadores", estadoMongo));
        d.addClassName("sc-capas");
        return d;
    }

    private static Div capa(String titulo, String tecnologia, String detalle, Div estado) {
        H4 h = new H4(titulo);
        Span t = new Span(tecnologia);
        t.addClassName("tecnologia");
        Span de = new Span(detalle);
        de.addClassName("detalle");
        Div c = new Div(h, t, de);
        if (estado != null) {
            estado.addClassName("estado");
            c.add(estado);
        }
        c.addClassName("capa");
        return c;
    }

    private static Div flecha(String texto) {
        Div f = new Div(new Span(texto));
        f.addClassName("flecha");
        return f;
    }

    private void comprobar() {
        Instant inicio = Instant.now();
        try {
            api.resumen();
            long ms = Duration.between(inicio, Instant.now()).toMillis();
            estadoApi.setText("Responde (" + ms + " ms)");
            estadoApi.removeClassName("caido");
            estadoMongo.setText("Conectada (la API leyó datos)");
            estadoMongo.removeClassName("caido");
        } catch (ApiException e) {
            estadoApi.setText(e.esSinConexion() ? "Sin conexión" : "Error " + e.getStatus());
            estadoApi.addClassName("caido");
            estadoMongo.setText("Sin datos: revisa la API");
            estadoMongo.addClassName("caido");
        }
    }

    private Div consola() {
        List<Prueba> pruebas = List.of(
                new Prueba("GET /api/reportes/resumen", api::resumen),
                new Prueba("GET /api/usuarios/tecnicos", api::tecnicos),
                new Prueba("GET /api/incidentes?estado=ABIERTO", () -> api.incidentes(Map.of("estado", "ABIERTO"))),
                new Prueba("GET /api/reportes/problemas-recurrentes?dias=30&minimo=3", () -> api.recurrentes(30, 3)),
                new Prueba("GET /api/reportes/tiempos?dias=30", () -> api.tiempos(30)),
                new Prueba("GET /api/incidentes/INC-999999  (error 404)", () -> api.incidente("INC-999999")),
                new Prueba("POST /api/incidentes con datos vacíos  (error 400)", () -> api.reportar(Map.of("titulo", ""))));
        Select<Prueba> elegir = new Select<>();
        elegir.setItems(pruebas);
        elegir.setItemLabelGenerator(Prueba::nombre);
        elegir.setValue(pruebas.get(0));
        elegir.setWidth("440px");
        elegir.setAriaLabel("Solicitud de ejemplo");
        Button enviar = new Button("Enviar solicitud", e -> {
            try {
                elegir.getValue().llamada().get();
            } catch (ApiException ex) {
                // la respuesta de error tambien queda en la bitacora
            }
            refrescar(true);
        });
        enviar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Div d = new Div(elegir, enviar);
        d.addClassName("sc-consola");
        return d;
    }

    private Grid<RegistroHttp.Entrada> bitacora() {
        bitacora.addColumn(RegistroHttp.Entrada::numero).setHeader("#").setWidth("70px").setFlexGrow(0);
        bitacora.addColumn(e -> Formato.fechaHora(e.fecha()).replaceAll("^.*, ", "")).setHeader("Hora").setWidth("90px").setFlexGrow(0);
        bitacora.addComponentColumn(e -> {
            Span s = new Span(e.metodo());
            s.addClassNames("sc-metodo", e.metodo().toLowerCase());
            return s;
        }).setHeader("Método").setWidth("100px").setFlexGrow(0);
        bitacora.addColumn(e -> e.url().replace(api.getUrlBase(), "")).setHeader("Ruta").setFlexGrow(3);
        bitacora.addComponentColumn(e -> {
            Span s = new Span(String.valueOf(e.status()));
            s.addClassName(e.exitosa() ? "sc-ok" : "sc-mal");
            return s;
        }).setHeader("Respuesta").setWidth("110px").setFlexGrow(0);
        bitacora.addColumn(e -> e.milisegundos() + " ms").setHeader("Duración").setWidth("100px").setFlexGrow(0);
        bitacora.setHeight("320px");
        bitacora.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        bitacora.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(this::mostrar));
        return bitacora;
    }

    private Div detalle() {
        cabeceraDetalle.addClassName("sc-detalle-http-cabecera");
        Div izq = new Div(new H4("Solicitud"), solicitud);
        Div der = new Div(new H4("Respuesta"), respuesta);
        Div lados = new Div(izq, der);
        lados.addClassName("sc-detalle-http-lados");
        Div d = new Div(cabeceraDetalle, lados);
        d.addClassName("sc-detalle-http");
        return d;
    }

    private void mostrar(RegistroHttp.Entrada e) {
        cabeceraDetalle.setText(e.metodo() + " " + e.url() + "  →  " + e.status() + " en " + e.milisegundos() + " ms");
        solicitud.setText(e.metodo() + " " + e.url() + "\nContent-Type: application/json\n\n"
                + (e.cuerpoSolicitud().isBlank() ? "(sin cuerpo)" : bonito(e.cuerpoSolicitud())));
        respuesta.setText("HTTP " + e.status() + "\n\n" + (e.cuerpoRespuesta().isBlank() ? "(sin cuerpo)" : bonito(e.cuerpoRespuesta())));
    }

    private String bonito(String texto) {
        try {
            return json.writeValueAsString(json.readTree(texto));
        } catch (Exception e) {
            return texto;
        }
    }

    private void refrescar(boolean seleccionarUltima) {
        List<RegistroHttp.Entrada> entradas = registro.ultimas();
        long numero = entradas.isEmpty() ? 0 : entradas.get(0).numero();
        if (numero != ultimoNumero) {
            bitacora.setItems(entradas);
            ultimoNumero = numero;
            if (seleccionarUltima && !entradas.isEmpty()) bitacora.select(entradas.get(0));
        }
    }

    private com.vaadin.flow.shared.Registration sondeo;

    @Override
    protected void onAttach(AttachEvent event) {
        refrescar(false);
        event.getUI().setPollInterval(2000);
        sondeo = event.getUI().addPollListener(e -> refrescar(false));
    }

    @Override
    protected void onDetach(DetachEvent event) {
        event.getUI().setPollInterval(-1);
        if (sondeo != null) sondeo.remove();
    }
}
