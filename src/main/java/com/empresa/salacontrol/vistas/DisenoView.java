package com.empresa.salacontrol.vistas;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las 5 vistas de diseno del proyecto y en que pantalla de la sala de control se ve cada una funcionando.
 */
@Route(value = "diseno", layout = MainLayout.class)
@PageTitle("Diseño del sistema: las 5 vistas")
public class DisenoView extends Div {

    private record Vista(String nombre, String imagen, String explicacion, List<Enlace> enlaces) { }

    private record Enlace(String texto, Class<? extends Component> destino) { }

    private final Div contenido = new Div();

    public DisenoView() {
        addClassNames("sc-vista", "sc-diseno");
        List<Vista> vistas = List.of(
                new Vista("Contexto", "vista-contexto.png",
                        "El sistema y sus actores externos: empleado, técnico, administrador y gerencia de TI. En la sala de "
                                + "control, el selector \"Actuando como\" de la barra superior elige el actor que realiza cada acción.",
                        List.of(new Enlace("Reportar incidente (empleado)", ReportarView.class),
                                new Enlace("Ciclo de vida (técnico y administrador)", IncidentesView.class),
                                new Enlace("Recurrencias y tiempos (gerencia)", ReportesView.class))),
                new Vista("Funcional (componentes)", "vista-funcional.png",
                        "Los componentes Gestión de Incidentes, Gestión de Usuarios y Reportes e Indicadores exponen las "
                                + "interfaces /api/incidentes, /api/usuarios y /api/reportes. El menú lateral está organizado igual.",
                        List.of(new Enlace("Gestión de incidentes", TableroView.class),
                                new Enlace("Gestión de usuarios", UsuariosView.class),
                                new Enlace("Reportes e indicadores", ReportesView.class))),
                new Vista("Lógica (clases)", "vista-logica.png",
                        "Incidente con su historial y comentarios embebidos, Usuario y los enumerados. La máquina de estados "
                                + "de EstadoIncidente define las columnas del tablero y qué movimientos se permiten.",
                        List.of(new Enlace("Tablero del ciclo de vida", IncidentesView.class))),
                new Vista("Desarrollo (paquetes)", "vista-desarrollo.png",
                        "Capas del backend: controller, service, repository y model. El frontend repite la idea: vistas "
                                + "(vistas), cliente HTTP (api) y piezas de interfaz (ui).",
                        List.of(new Enlace("Bitácora de solicitudes a los controladores", ArquitecturaView.class))),
                new Vista("Despliegue", "vista-despliegue.png",
                        "Nodos donde corre cada parte y cómo se comunican: navegador, servidor del frontend, servidor de la "
                                + "API por HTTP/JSON y MongoDB por TCP 27017.",
                        List.of(new Enlace("Arquitectura en vivo", ArquitecturaView.class))));

        Map<Tab, Vista> porPestana = new LinkedHashMap<>();
        Tabs pestanas = new Tabs();
        for (Vista v : vistas) {
            Tab t = new Tab(v.nombre());
            porPestana.put(t, v);
            pestanas.add(t);
        }
        pestanas.addSelectedChangeListener(e -> mostrar(porPestana.get(e.getSelectedTab())));
        contenido.addClassName("sc-panel");
        add(pestanas, contenido);
        mostrar(vistas.get(0));
    }

    private void mostrar(Vista v) {
        contenido.removeAll();
        Paragraph texto = new Paragraph(v.explicacion());
        UnorderedList enlaces = new UnorderedList();
        for (Enlace e : v.enlaces()) {
            enlaces.add(new ListItem(new Anchor(RouteConfiguration.forSessionScope().getUrl(e.destino()), e.texto())));
        }
        Div lado = new Div(texto, new Paragraph("Dónde verlo funcionando:"), enlaces);
        lado.addClassName("lado");
        Image img = new Image("diagramas/" + v.imagen(), "Diagrama de la vista " + v.nombre());
        Div marco = new Div(img);
        marco.addClassName("marco");
        Div d = new Div(lado, marco);
        d.addClassName("sc-diseno-cuerpo");
        contenido.add(d);
    }
}
