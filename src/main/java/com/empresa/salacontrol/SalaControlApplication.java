package com.empresa.salacontrol;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Frontend "Sala de control" del Sistema de Gestion de Incidentes de TI.
 * Toda la interfaz esta escrita en Java con Vaadin; los datos se obtienen de la API REST del backend por HTTP.
 */
@SpringBootApplication
@Theme("salacontrol")
@Meta(name = "description", content = "Sala de control del Sistema de Gestión de Incidentes de TI")
public class SalaControlApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(SalaControlApplication.class, args);
    }
}
