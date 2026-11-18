package com.empresa.salacontrol.ui;

import com.empresa.salacontrol.api.ApiCliente;
import com.empresa.salacontrol.api.ApiException;
import com.empresa.salacontrol.api.Dto;
import com.empresa.salacontrol.sesion.SesionUsuario;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.function.Consumer;

/**
 * Mueve un incidente a otro estado respetando las reglas de la API:
 * ABIERTO -> ASIGNADO pide un tecnico (PATCH /asignar), -> RESUELTO pide la solucion (PATCH /resolver)
 * y el resto usa PATCH /estado con un motivo opcional.
 */
public final class Acciones {

    private Acciones() {
    }

    public static void mover(ApiCliente api, SesionUsuario sesion, Dto.Incidente inc, String destino,
                             Consumer<Dto.Incidente> alTerminar) {
        if (!sesion.hay()) {
            Piezas.error(new ApiException(422, "Elige arriba con qué usuario estás actuando", null));
            return;
        }
        if (!inc.siguientesEstados().contains(destino)) {
            // Validacion local con la misma maquina de estados que devuelve la API (siguientesEstados);
            // la API vuelve a validar cada cambio y responde 409 si no es valido.
            Piezas.error(new ApiException(422, "Movimiento no permitido: desde " + Formato.estado(inc.estado()) + " no se puede pasar a "
                    + Formato.estado(destino) + ". Pasos permitidos: "
                    + String.join(", ", inc.siguientesEstados().stream().map(Formato::estado).toList()), null));
            return;
        }
        switch (destino) {
            case "ASIGNADO" -> elegirTecnico(api, sesion, inc, alTerminar);
            case "RESUELTO" -> pedirSolucion(api, sesion, inc, alTerminar);
            default -> pedirMotivo(api, sesion, inc, destino, alTerminar);
        }
    }

    public static void elegirTecnico(ApiCliente api, SesionUsuario sesion, Dto.Incidente inc,
                                     Consumer<Dto.Incidente> alTerminar) {
        Dialog d = dialogo("Asignar " + inc.codigo() + " a un técnico");
        ComboBox<Dto.Usuario> tecnico = new ComboBox<>("Técnico");
        try {
            tecnico.setItems(api.tecnicos());
        } catch (ApiException e) {
            Piezas.error(e);
            return;
        }
        tecnico.setItemLabelGenerator(t -> t.nombre() + (t.especialidad() != null
                ? " (" + Formato.categoria(t.especialidad()) + ")" : ""));
        tecnico.setWidthFull();
        d.add(new Paragraph("Categoría del incidente: " + Formato.categoria(inc.categoria())), tecnico);
        Button auto = new Button("Elegir automáticamente", e -> ejecutar(d, alTerminar,
                () -> api.asignarAutomatico(inc.id(), sesion.id()), "asignado"));
        Button asignar = new Button("Asignar", e -> {
            if (tecnico.getValue() == null) { tecnico.setInvalid(true); tecnico.setErrorMessage("Elige un técnico"); return; }
            ejecutar(d, alTerminar, () -> api.asignar(inc.id(), tecnico.getValue().id(), sesion.id()), "asignado");
        });
        asignar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        d.getFooter().add(new Button("Cancelar", e -> d.close()), auto, asignar);
        d.open();
    }

    private static void pedirSolucion(ApiCliente api, SesionUsuario sesion, Dto.Incidente inc,
                                      Consumer<Dto.Incidente> alTerminar) {
        Dialog d = dialogo("Resolver " + inc.codigo());
        TextArea solucion = new TextArea("Solución aplicada");
        solucion.setWidthFull();
        solucion.setMinHeight("120px");
        solucion.setHelperText("Queda en el historial y sirve de antecedente para futuras fallas del mismo activo.");
        d.add(solucion);
        Button ok = new Button("Marcar como resuelto", e -> {
            if (solucion.getValue().isBlank()) { solucion.setInvalid(true); solucion.setErrorMessage("Describe la solución"); return; }
            ejecutar(d, alTerminar, () -> api.resolver(inc.id(), solucion.getValue(), sesion.id()), "resuelto");
        });
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        d.getFooter().add(new Button("Cancelar", e -> d.close()), ok);
        d.open();
    }

    private static void pedirMotivo(ApiCliente api, SesionUsuario sesion, Dto.Incidente inc, String destino,
                                    Consumer<Dto.Incidente> alTerminar) {
        Dialog d = dialogo("Pasar " + inc.codigo() + " a " + Formato.estado(destino));
        TextField motivo = new TextField("Motivo (opcional)");
        motivo.setWidthFull();
        motivo.setPlaceholder(switch (destino) {
            case "EN_ESPERA" -> "Por ejemplo: esperando repuesto";
            case "CERRADO" -> "Por ejemplo: confirmado por el usuario";
            case "CANCELADO" -> "Por ejemplo: duplicado";
            case "EN_PROGRESO" -> "RESUELTO".equals(inc.estado()) ? "Por ejemplo: la falla volvió" : "Por ejemplo: inicio del diagnóstico";
            default -> "";
        });
        d.add(motivo);
        Button ok = new Button("Pasar a " + Formato.estado(destino), e -> ejecutar(d, alTerminar,
                () -> api.cambiarEstado(inc.id(), destino, sesion.id(), motivo.getValue().isBlank() ? null : motivo.getValue()),
                "ahora está " + Formato.estado(destino).toLowerCase()));
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        if ("CANCELADO".equals(destino)) ok.addThemeVariants(ButtonVariant.LUMO_ERROR);
        d.getFooter().add(new Button("Cancelar", e -> d.close()), ok);
        d.open();
        motivo.focus();
    }

    private static Dialog dialogo(String titulo) {
        Dialog d = new Dialog();
        d.setHeaderTitle(titulo);
        d.setWidth("440px");
        d.addClassName("sc-dialogo");
        return d;
    }

    private static void ejecutar(Dialog d, Consumer<Dto.Incidente> alTerminar,
                                 java.util.function.Supplier<Dto.Incidente> llamada, String verbo) {
        try {
            Dto.Incidente r = llamada.get();
            d.close();
            Piezas.ok(r.codigo() + " " + verbo);
            alTerminar.accept(r);
        } catch (ApiException e) {
            Piezas.error(e);
        }
    }
}
