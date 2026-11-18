package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.ui.Formato;
import com.empresa.salacontrol.ui.Piezas;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion de usuarios (reportantes, tecnicos y administradores) y carga de trabajo de cada tecnico.
 * Usa GET/POST/DELETE /api/usuarios y GET /api/reportes/tecnicos.
 */
@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Usuarios y técnicos")
public class UsuariosView extends Div {

    private final ApiCliente api;
    private final Grid<Dto.Usuario> tabla = new Grid<>();
    private final Div carga = new Div();

    public UsuariosView(ApiCliente api) {
        this.api = api;
        addClassNames("sc-vista", "sc-usuarios");

        tabla.addColumn(Dto.Usuario::nombre).setHeader("Nombre").setAutoWidth(true).setSortable(true);
        tabla.addColumn(Dto.Usuario::email).setHeader("Email").setAutoWidth(true);
        tabla.addColumn(u -> Formato.rol(u.rol())).setHeader("Rol").setSortable(true);
        tabla.addColumn(Dto.Usuario::departamento).setHeader("Departamento");
        tabla.addColumn(u -> u.especialidad() == null ? "" : Formato.categoria(u.especialidad())).setHeader("Especialidad");
        tabla.addComponentColumn(u -> {
            Span s = new Span(u.activo() ? "Activo" : "Inactivo");
            s.addClassName(u.activo() ? "sc-activo" : "sc-inactivo");
            return s;
        }).setHeader("Estado");
        tabla.addComponentColumn(u -> {
            Button b = new Button("Desactivar", e -> desactivar(u));
            b.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            b.setEnabled(u.activo());
            return b;
        }).setAutoWidth(true);
        tabla.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        tabla.setAllRowsVisible(true);

        Button nuevo = new Button("Registrar usuario", e -> abrirFormulario());
        nuevo.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        carga.addClassName("sc-carga");
        add(Piezas.panel("Carga de trabajo por técnico", "Incidentes pendientes (asignados, en progreso o en espera) "
                + "frente a los ya resueltos.", carga));
        Div acciones = new Div(nuevo);
        acciones.addClassName("sc-acciones");
        add(Piezas.panel("Personas registradas", "La baja es lógica: el usuario se conserva para no romper el historial.",
                acciones, tabla));
        cargar();
    }

    private void cargar() {
        try {
            tabla.setItems(api.usuarios().stream()
                    .sorted(Comparator.comparing((Dto.Usuario u) -> !u.activo()).thenComparing(Dto.Usuario::rol)
                            .thenComparing(Dto.Usuario::nombre)).toList());
            carga.removeAll();
            List<Dto.Desempeno> tecnicos = api.desempeno();
            long maximo = Math.max(1, tecnicos.stream().mapToLong(Dto.Desempeno::asignados).max().orElse(1));
            if (tecnicos.isEmpty()) carga.add(Piezas.vacio("No hay técnicos activos."));
            for (Dto.Desempeno t : tecnicos) {
                Span nombre = new Span(t.nombre());
                nombre.addClassName("nombre");
                Div barra = new Div();
                barra.addClassName("barra");
                Span pendientes = new Span();
                pendientes.addClassName("pendientes");
                pendientes.getStyle().set("width", (100.0 * t.activos() / maximo) + "%");
                Span resueltos = new Span();
                resueltos.addClassName("resueltos");
                resueltos.getStyle().set("width", (100.0 * t.resueltos() / maximo) + "%");
                barra.add(pendientes, resueltos);
                Span detalle = new Span(t.activos() + " pendientes, " + t.resueltos() + " resueltos, promedio "
                        + Formato.minutos(t.promedioMinutosResolucion())
                        + (t.slaIncumplidos() > 0 ? ", " + t.slaIncumplidos() + " fuera de SLA" : ""));
                detalle.addClassName("detalle");
                Div fila = new Div(nombre, barra, detalle);
                fila.addClassName("fila");
                carga.add(fila);
            }
        } catch (ApiException e) {
            Piezas.error(e);
        }
    }

    private void desactivar(Dto.Usuario u) {
        try {
            api.desactivarUsuario(u.id());
            Piezas.ok(u.nombre() + " desactivado");
            cargar();
        } catch (ApiException e) {
            Piezas.error(e);
        }
    }

    private void abrirFormulario() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Registrar usuario");
        d.setWidth("460px");
        TextField nombre = new TextField("Nombre completo");
        EmailField email = new EmailField("Email");
        Select<String> rol = new Select<>();
        rol.setLabel("Rol");
        rol.setItems("REPORTANTE", "TECNICO", "ADMINISTRADOR");
        rol.setItemLabelGenerator(r -> switch (r) {
            case "REPORTANTE" -> "Empleado (reporta problemas)";
            case "TECNICO" -> "Técnico de soporte";
            default -> "Administrador de mesa de ayuda";
        });
        rol.setValue("REPORTANTE");
        TextField depto = new TextField("Departamento");
        Select<String> especialidad = new Select<>();
        especialidad.setLabel("Especialidad");
        especialidad.setItems(Formato.CATEGORIAS);
        especialidad.setItemLabelGenerator(Formato::categoria);
        especialidad.setVisible(false);
        rol.addValueChangeListener(e -> especialidad.setVisible("TECNICO".equals(e.getValue())));
        for (var c : List.of(nombre, email, depto)) c.setWidthFull();
        rol.setWidthFull();
        especialidad.setWidthFull();
        d.add(new Div(nombre, email, rol, especialidad, depto));
        Button guardar = new Button("Registrar", e -> {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("nombre", nombre.getValue());
            datos.put("email", email.getValue());
            datos.put("rol", rol.getValue());
            datos.put("departamento", depto.getValue());
            if ("TECNICO".equals(rol.getValue())) datos.put("especialidad", especialidad.getValue());
            try {
                Dto.Usuario u = api.crearUsuario(datos);
                d.close();
                Piezas.ok(u.nombre() + " registrado");
                cargar();
            } catch (ApiException ex) {
                Piezas.error(ex);
            }
        });
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        d.getFooter().add(new Button("Cancelar", e -> d.close()), guardar);
        d.open();
    }
}
