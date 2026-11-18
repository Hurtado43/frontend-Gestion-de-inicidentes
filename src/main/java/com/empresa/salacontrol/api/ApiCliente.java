package com.empresa.salacontrol.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Cliente HTTP de la API REST del backend. Cada metodo corresponde a un endpoint
 * (ver README del backend) y todas las llamadas quedan en el {@link RegistroHttp}.
 */
@Service
public class ApiCliente {

    private final RestClient http;
    private final ObjectMapper json;
    private final String urlBase;

    public ApiCliente(@Value("${api.url}") String urlBase, RegistroHttp registro, ObjectMapper json) {
        this.urlBase = urlBase;
        this.json = json;
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        // El buffer permite leer el cuerpo de la respuesta para la bitacora sin consumirlo
        var fabrica = new BufferingClientHttpRequestFactory(new JdkClientHttpRequestFactory(cliente));
        this.http = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .requestInterceptor((solicitud, cuerpo, ejecucion) -> {
                    long inicio = System.nanoTime();
                    ClientHttpResponse respuesta = ejecucion.execute(solicitud, cuerpo);
                    long ms = (System.nanoTime() - inicio) / 1_000_000;
                    String texto = StreamUtils.copyToString(respuesta.getBody(), StandardCharsets.UTF_8);
                    registro.registrar(solicitud.getMethod().name(), solicitud.getURI().toString(),
                            respuesta.getStatusCode().value(), ms, new String(cuerpo, StandardCharsets.UTF_8), texto);
                    return respuesta;
                })
                .defaultStatusHandler(HttpStatusCode::isError, (solicitud, respuesta) -> {
                    throw convertirError(respuesta);
                })
                .build();
    }

    public String getUrlBase() {
        return urlBase;
    }

    // ------------------------------------------------------------------ usuarios

    public List<Dto.Usuario> usuarios() {
        return llamar(() -> http.get().uri("/api/usuarios").retrieve().body(new ParameterizedTypeReference<>() {}));
    }

    public List<Dto.Usuario> tecnicos() {
        return llamar(() -> http.get().uri("/api/usuarios/tecnicos").retrieve().body(new ParameterizedTypeReference<>() {}));
    }

    public Dto.Usuario crearUsuario(Map<String, Object> datos) {
        return llamar(() -> http.post().uri("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                .body(datos).retrieve().body(Dto.Usuario.class));
    }

    public void desactivarUsuario(String id) {
        llamar(() -> http.delete().uri("/api/usuarios/{id}", id).retrieve().toBodilessEntity());
    }

    // ------------------------------------------------------------------ incidentes

    public List<Dto.Incidente> incidentes(Map<String, String> filtros) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromPath("/api/incidentes");
        filtros.forEach((k, v) -> { if (v != null && !v.isBlank()) uri.queryParam(k, v); });
        String ruta = uri.build().encode().toUriString();
        return llamar(() -> http.get().uri(ruta).retrieve().body(new ParameterizedTypeReference<>() {}));
    }

    public Dto.Incidente incidente(String idOCodigo) {
        return llamar(() -> http.get().uri("/api/incidentes/{id}", idOCodigo).retrieve().body(Dto.Incidente.class));
    }

    public Dto.Incidente reportar(Map<String, Object> datos) {
        return llamar(() -> http.post().uri("/api/incidentes").contentType(MediaType.APPLICATION_JSON)
                .body(datos).retrieve().body(Dto.Incidente.class));
    }

    public Dto.Incidente asignar(String id, String tecnicoId, String usuarioId) {
        return patch("/api/incidentes/{id}/asignar", id, cuerpo("tecnicoId", tecnicoId, "usuarioId", usuarioId));
    }

    public Dto.Incidente asignarAutomatico(String id, String usuarioId) {
        return llamar(() -> http.patch().uri("/api/incidentes/{id}/asignar-automatico?usuarioId={u}", id, usuarioId)
                .retrieve().body(Dto.Incidente.class));
    }

    public Dto.Incidente cambiarEstado(String id, String estado, String usuarioId, String motivo) {
        return patch("/api/incidentes/{id}/estado", id, cuerpo("estado", estado, "usuarioId", usuarioId, "motivo", motivo));
    }

    public Dto.Incidente cambiarPrioridad(String id, String prioridad, String usuarioId) {
        return patch("/api/incidentes/{id}/prioridad", id, cuerpo("prioridad", prioridad, "usuarioId", usuarioId));
    }

    public Dto.Incidente resolver(String id, String solucion, String usuarioId) {
        return patch("/api/incidentes/{id}/resolver", id, cuerpo("solucion", solucion, "usuarioId", usuarioId));
    }

    public Dto.Incidente comentar(String id, String autorId, String texto) {
        return llamar(() -> http.post().uri("/api/incidentes/{id}/comentarios", id).contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo("autorId", autorId, "texto", texto)).retrieve().body(Dto.Incidente.class));
    }

    public List<Dto.Incidente> antecedentes(String id) {
        return llamar(() -> http.get().uri("/api/incidentes/{id}/antecedentes", id).retrieve()
                .body(new ParameterizedTypeReference<>() {}));
    }

    // ------------------------------------------------------------------ reportes

    public Dto.Resumen resumen() {
        return llamar(() -> http.get().uri("/api/reportes/resumen").retrieve().body(Dto.Resumen.class));
    }

    public List<Dto.Recurrente> recurrentes(int dias, int minimo) {
        return llamar(() -> http.get().uri("/api/reportes/problemas-recurrentes?dias={d}&minimo={m}", dias, minimo)
                .retrieve().body(new ParameterizedTypeReference<>() {}));
    }

    public List<Dto.Tiempos> tiempos(int dias) {
        return llamar(() -> http.get().uri("/api/reportes/tiempos?dias={d}", dias).retrieve()
                .body(new ParameterizedTypeReference<>() {}));
    }

    public List<Dto.Desempeno> desempeno() {
        return llamar(() -> http.get().uri("/api/reportes/tecnicos").retrieve()
                .body(new ParameterizedTypeReference<>() {}));
    }

    // ------------------------------------------------------------------ auxiliares

    private Dto.Incidente patch(String ruta, String id, Map<String, Object> datos) {
        return llamar(() -> http.patch().uri(ruta, id).contentType(MediaType.APPLICATION_JSON)
                .body(datos).retrieve().body(Dto.Incidente.class));
    }

    private static Map<String, Object> cuerpo(Object... claveValor) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            if (claveValor[i + 1] != null) m.put((String) claveValor[i], claveValor[i + 1]);
        }
        return m;
    }

    private <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (ResourceAccessException e) {
            throw new ApiException(0, "No hay conexión con la API en " + urlBase
                    + ". Verifica que el backend (Spring Boot) y MongoDB estén en ejecución.", List.of());
        }
    }

    private ApiException convertirError(ClientHttpResponse respuesta) throws IOException {
        int status = respuesta.getStatusCode().value();
        String texto = StreamUtils.copyToString(respuesta.getBody(), StandardCharsets.UTF_8);
        try {
            Dto.Error error = json.readValue(texto, Dto.Error.class);
            if (error.mensaje() != null) return new ApiException(status, error.mensaje(), error.detalles());
        } catch (Exception ignorado) {
            // la respuesta no tenia el formato de error de la API
        }
        return new ApiException(status, "La API respondió " + status, List.of());
    }
}
