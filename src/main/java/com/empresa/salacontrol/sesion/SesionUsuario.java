package com.empresa.salacontrol.sesion;

import com.empresa.salacontrol.api.Dto;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Usuario con el que se esta "actuando" en esta sesion del navegador.
 * La API no tiene autenticacion: cada operacion envia el id de quien la realiza (usuarioId),
 * y ese id sale de aqui. Corresponde a los actores del diagrama de contexto.
 */
@Component
@VaadinSessionScope
public class SesionUsuario {

    private Dto.Usuario actual;
    private final List<Consumer<Dto.Usuario>> oyentes = new ArrayList<>();

    public Dto.Usuario getActual() {
        return actual;
    }

    public String id() {
        return actual == null ? null : actual.id();
    }

    public boolean hay() {
        return actual != null;
    }

    public void setActual(Dto.Usuario usuario) {
        this.actual = usuario;
        new ArrayList<>(oyentes).forEach(o -> o.accept(usuario));
    }

    public Runnable alCambiar(Consumer<Dto.Usuario> oyente) {
        oyentes.add(oyente);
        return () -> oyentes.remove(oyente);
    }
}
