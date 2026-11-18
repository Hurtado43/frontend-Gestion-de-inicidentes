package com.empresa.salacontrol.api;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bitacora en memoria de las solicitudes y respuestas HTTP que el frontend envia a la API.
 * La vista "Arquitectura" la muestra en vivo, porque en Vaadin las llamadas viajan de servidor a servidor
 * y no aparecen en las herramientas de desarrollo del navegador.
 */
@Component
public class RegistroHttp {

    private static final int MAXIMO = 200;

    public record Entrada(long numero, Instant fecha, String metodo, String url, int status, long milisegundos,
                          String cuerpoSolicitud, String cuerpoRespuesta) {
        public boolean exitosa() {
            return status >= 200 && status < 300;
        }
    }

    private final Deque<Entrada> entradas = new ArrayDeque<>();
    private final AtomicLong contador = new AtomicLong();

    public synchronized void registrar(String metodo, String url, int status, long ms, String solicitud, String respuesta) {
        entradas.addFirst(new Entrada(contador.incrementAndGet(), Instant.now(), metodo, url, status, ms,
                recortar(solicitud), recortar(respuesta)));
        while (entradas.size() > MAXIMO) entradas.removeLast();
    }

    public synchronized List<Entrada> ultimas() {
        return new ArrayList<>(entradas);
    }

    public synchronized void limpiar() {
        entradas.clear();
    }

    private static String recortar(String texto) {
        if (texto == null) return "";
        return texto.length() > 6000 ? texto.substring(0, 6000) + "\n… (recortado)" : texto;
    }
}
