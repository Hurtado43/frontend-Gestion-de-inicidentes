package com.empresa.salacontrol.vistas;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.sesion.SesionUsuario;
import com.empresa.salacontrol.ui.Acciones;
import com.empresa.salacontrol.ui.Formato;
import com.empresa.salacontrol.ui.Piezas;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tablero del ciclo de vida: una columna por estado del incidente (maquina de estados del diagrama de clases).
 * Se puede arrastrar una tarjeta a otra columna o usar su menu "Mover a"; solo se ofrecen los pasos que la API
 * permite (siguientesEstados) y la API valida de nuevo cada cambio.
 */
@Route(value = "incidentes", layout = MainLayout.class)
@PageTitle("Ciclo de vida de los incidentes")
public class IncidentesView extends Div implements BeforeEnterObserver {

    private static final List<String> COLUMNAS =
            List.of("ABIERTO", "ASIGNADO", "EN_PROGRESO", "EN_ESPERA", "RESUELTO", "CERRADO", "CANCELADO");

    private final ApiCliente api;
    private final SesionUsuario sesion;

    private final Select<String> prioridad = new Select<>();
    private final Select<String> categoria = new Select<>();
    private final ComboBox<Dto.Usuario> tecnico = new ComboBox<>("Técnico");
    private final TextField texto = new TextField("Buscar");
    private final TextField activo = new TextField("Activo afectado");
    private final Div tablero = new Div();
    private final Map<String, Div> columnas = new LinkedHashMap<>();
    private Map<String, Dto.Usuario> usuarios = new HashMap<>();

    public IncidentesView(ApiCliente api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
        addClassNames("sc-vista", "sc-vista-ancha");

        prioridad.setLabel("Prioridad");
        prioridad.setItems(conTodas(Formato.PRIORIDADES));
        prioridad.setItemLabelGenerator(p -> p.isEmpty() ? "Todas" : Formato.prioridad(p));
        prioridad.setValue("");
        categoria.setLabel("Categoría");
        categoria.setItems(conTodas(Formato.CATEGORIAS));
        categoria.setItemLabelGenerator(c -> c.isEmpty() ? "Todas" : Formato.categoria(c));
        categoria.setValue("");
        tecnico.setItemLabelGenerator(Dto.Usuario::nombre);
        tecnico.setClearButtonVisible(true);
        tecnico.setPlaceholder("Todos");
        texto.setPlaceholder("Título o descripción");
        texto.setValueChangeMode(ValueChangeMode.LAZY);
        texto.setClearButtonVisible(true);
        activo.setPlaceholder("Ej. ROUTER-PISO3");
        activo.setValueChangeMode(ValueChangeMode.LAZY);
        activo.setClearButtonVisible(true);
        prioridad.addValueChangeListener(e -> cargar());
        categoria.addValueChangeListener(e -> cargar());
        tecnico.addValueChangeListener(e -> cargar());
        texto.addValueChangeListener(e -> cargar());
        activo.addValueChangeListener(e -> cargar());

        Div filtros = new Div(texto, activo, prioridad, categoria, tecnico);
        filtros.addClassName("sc-filtros");
        Span ayuda = new Span("Arrastra una tarjeta a otra columna o usa su menú. Las columnas que no se pueden "
                + "alcanzar se atenúan mientras arrastras.");
        ayuda.addClassName("sc-ayuda-linea");

        tablero.addClassName("sc-kanban");
        for (String estado : COLUMNAS) tablero.add(columna(estado));
        add(filtros, ayuda, tablero);
    }

    /** Lista de opciones con "" al inicio, que significa "Todas". */
    private static List<String> conTodas(List<String> valores) {
        List<String> l = new ArrayList<>();
        l.add("");
        l.addAll(valores);
        return l;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter("activo").ifPresent(activo::setValue);
        cargar();
    }

    private Div columna(String estado) {
        H4 titulo = new H4(Formato.estado(estado));
        Span cuenta = new Span("0");
        cuenta.addClassName("cuenta");
        Div cabecera = new Div(titulo, cuenta);
        cabecera.addClassNames("cabecera", Formato.claseEstado(estado));
        Div tarjetas = new Div();
        tarjetas.addClassName("tarjetas");
        Div col = new Div(cabecera, tarjetas);
        col.addClassName("columna");
        col.getElement().setAttribute("data-estado", estado);
        if ("CANCELADO".equals(estado)) col.addClassName("estrecha");

        DropTarget<Div> destino = DropTarget.create(col);
        destino.setDropEffect(DropEffect.MOVE);
        destino.addDropListener(e -> e.getDragData().ifPresent(dato -> {
            Dto.Incidente inc = (Dto.Incidente) dato;
            if (!inc.estado().equals(estado)) Acciones.mover(api, sesion, inc, estado, r -> cargar());
        }));
        columnas.put(estado, col);
        return col;
    }

    private void cargar() {
        try {
            usuarios = api.usuarios().stream().collect(Collectors.toMap(Dto.Usuario::id, Function.identity()));
            if (tecnico.getListDataView().getItemCount() == 0) {
                Dto.Usuario elegido = tecnico.getValue();
                tecnico.setItems(usuarios.values().stream().filter(Dto.Usuario::esTecnico).toList());
                tecnico.setValue(elegido);
            }
            Map<String, String> filtros = new HashMap<>();
            filtros.put("prioridad", prioridad.getValue());
            filtros.put("categoria", categoria.getValue());
            filtros.put("texto", texto.getValue());
            if (tecnico.getValue() != null) filtros.put("tecnicoId", tecnico.getValue().id());
            List<Dto.Incidente> lista = api.incidentes(filtros);
            String filtroActivo = activo.getValue().trim().toUpperCase();
            if (!filtroActivo.isEmpty()) {
                lista = lista.stream().filter(i -> i.activoAfectado() != null && i.activoAfectado().contains(filtroActivo)).toList();
            }
            Map<String, List<Dto.Incidente>> porEstado = lista.stream().collect(Collectors.groupingBy(Dto.Incidente::estado));
            for (var c : columnas.entrySet()) {
                List<Dto.Incidente> deEstado = porEstado.getOrDefault(c.getKey(), List.of());
                Div col = c.getValue();
                Div tarjetas = (Div) col.getChildren().skip(1).findFirst().orElseThrow();
                tarjetas.removeAll();
                deEstado.forEach(i -> tarjetas.add(tarjeta(i)));
                if (deEstado.isEmpty()) tarjetas.add(Piezas.vacio("Sin incidentes"));
                ((Span) ((Div) col.getChildren().findFirst().orElseThrow()).getChildren().skip(1).findFirst().orElseThrow())
                        .setText(String.valueOf(deEstado.size()));
            }
        } catch (ApiException e) {
            Piezas.error(e);
        }
    }

    private Component tarjeta(Dto.Incidente i) {
        Anchor titulo = new Anchor(RouteConfiguration.forSessionScope().getUrl(DetalleIncidenteView.class, i.id()), i.titulo());
        titulo.addClassName("titulo");
        Span codigo = new Span(i.codigo());
        codigo.addClassName("codigo");

        MenuBar menu = new MenuBar();
        menu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE, MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_SMALL);
        MenuItem raiz = menu.addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        raiz.setAriaLabel("Mover " + i.codigo() + " a otro estado");
        menu.setTooltipText(raiz, "Mover a otro estado");
        SubMenu opciones = raiz.getSubMenu();
        if (i.siguientesEstados().isEmpty()) {
            opciones.addItem("Estado final: no admite cambios").setEnabled(false);
        }
        for (String destino : i.siguientesEstados()) {
            opciones.addItem("Mover a " + Formato.estado(destino), e -> Acciones.mover(api, sesion, i, destino, r -> cargar()));
        }

        Div cabeza = new Div(codigo, Piezas.prioridad(i.prioridad()), menu);
        cabeza.addClassName("cabeza");
        Span quien = new Span(Piezas.nombre(usuarios, i.tecnicoAsignadoId()));
        quien.addClassName(i.tecnicoAsignadoId() == null ? "sin-tecnico" : "tecnico");
        Div pie = new Div(quien, Piezas.sla(i));
        pie.addClassName("pie");
        Span activoTxt = new Span(Formato.categoria(i.categoria()) + (i.activoAfectado() != null ? ", " + i.activoAfectado() : ""));
        activoTxt.addClassName("activo");

        Div card = new Div(cabeza, titulo, activoTxt, pie);
        card.addClassNames("tarjeta", Formato.clasePrioridad(i.prioridad()));
        DragSource<Div> arrastre = DragSource.create(card);
        arrastre.setDragData(i);
        arrastre.addDragStartListener(e -> resaltar(i));
        arrastre.addDragEndListener(e -> limpiarResaltado());
        return card;
    }

    private void resaltar(Dto.Incidente i) {
        columnas.forEach((estado, col) -> {
            if (estado.equals(i.estado())) return;
            col.addClassName(i.siguientesEstados().contains(estado) ? "permitida" : "bloqueada");
        });
    }

    private void limpiarResaltado() {
        columnas.values().forEach(c -> c.removeClassNames("permitida", "bloqueada"));
    }
}
