package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.sesion.SesionUsuario;
import com.empresa.salacontrol.ui.Formato;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;

import java.util.Comparator;
import java.util.List;

/**
 * Estructura comun: barra lateral con las opciones (nombradas como los componentes del diagrama de
 * componentes) y barra superior con el selector de actor ("Actuando como") y el estado de la conexion.
 */
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final H2 titulo = new H2();
    private final Span conexion = new Span();
    private final ComboBox<Dto.Usuario> actor = new ComboBox<>();
    private final ApiCliente api;
    private final SesionUsuario sesion;

    public MainLayout(ApiCliente api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
        setPrimarySection(Section.DRAWER);

        Div marca = new Div(new H1("Sala de control"), new Paragraph("Gestión de incidentes de TI"));
        marca.addClassName("sc-marca");

        Div menu = new Div(marca,
                grupo("Gestión de incidentes"),
                nav(item("Tablero", TableroView.class, VaadinIcon.DASHBOARD),
                        item("Ciclo de vida", IncidentesView.class, VaadinIcon.GRID_BIG_O),
                        item("Reportar incidente", ReportarView.class, VaadinIcon.PLUS_CIRCLE_O)),
                grupo("Gestión de usuarios"),
                nav(item("Usuarios y técnicos", UsuariosView.class, VaadinIcon.USERS)),
                grupo("Reportes e indicadores"),
                nav(item("Recurrencias y tiempos", ReportesView.class, VaadinIcon.CHART_GRID)),
                grupo("Para el profesor"),
                nav(item("Arquitectura en vivo", ArquitecturaView.class, VaadinIcon.EXCHANGE),
                        item("Diseño (5 vistas)", DisenoView.class, VaadinIcon.SITEMAP)));
        addToDrawer(new Scroller(menu));

        actor.setPlaceholder("Elige un usuario");
        actor.setItemLabelGenerator(u -> u.nombre() + " (" + Formato.rol(u.rol()) + ")");
        actor.setWidth("300px");
        actor.setAriaLabel("Actuando como");
        actor.addValueChangeListener(e -> { if (e.getValue() != null) sesion.setActual(e.getValue()); });
        Span etiqueta = new Span("Actuando como");
        etiqueta.addClassName("sc-conexion-texto");
        etiqueta.getStyle().set("color", "var(--sc-tinta-suave)").set("font-size", "0.9rem");

        conexion.addClassName("sc-conexion");
        Div barra = new Div(titulo, etiqueta, actor, conexion);
        barra.addClassName("sc-barra");
        addToNavbar(new DrawerToggle(), barra);

        cargarActores();
    }

    private void cargarActores() {
        try {
            List<Dto.Usuario> usuarios = api.usuarios().stream().filter(Dto.Usuario::activo)
                    .sorted(Comparator.comparing((Dto.Usuario u) -> ordenRol(u.rol())).thenComparing(Dto.Usuario::nombre))
                    .toList();
            actor.setItems(usuarios);
            Dto.Usuario elegido = sesion.hay()
                    ? usuarios.stream().filter(u -> u.id().equals(sesion.id())).findFirst().orElse(null) : null;
            if (elegido == null) {
                elegido = usuarios.stream().filter(u -> "ADMINISTRADOR".equals(u.rol())).findFirst()
                        .orElse(usuarios.isEmpty() ? null : usuarios.get(0));
            }
            if (elegido != null) actor.setValue(elegido);
            conexion.setText("API conectada");
            conexion.removeClassName("caida");
            conexion.setTitle(api.getUrlBase());
        } catch (ApiException e) {
            conexion.setText("API sin conexión");
            conexion.addClassName("caida");
            conexion.setTitle(e.getMessage());
        }
    }

    private static int ordenRol(String rol) {
        return switch (rol) {
            case "ADMINISTRADOR" -> 0;
            case "TECNICO" -> 1;
            default -> 2;
        };
    }

    private static Div grupo(String texto) {
        Div d = new Div(texto);
        d.addClassName("sc-nav-grupo");
        return d;
    }

    private static SideNav nav(SideNavItem... items) {
        SideNav nav = new SideNav();
        nav.addClassName("sc-nav");
        nav.addItem(items);
        return nav;
    }

    private static SideNavItem item(String texto, Class<? extends com.vaadin.flow.component.Component> vista, VaadinIcon icono) {
        return new SideNavItem(texto, vista, icono.create());
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        PageTitle t = getContent() == null ? null : getContent().getClass().getAnnotation(PageTitle.class);
        titulo.setText(t == null ? "" : t.value());
        cargarActores();
    }
}
