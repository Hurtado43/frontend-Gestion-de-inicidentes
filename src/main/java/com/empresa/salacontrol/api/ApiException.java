package com.empresa.salacontrol.api;

import java.util.List;

/** Error devuelto por la API (400, 404, 409...) o fallo de conexion con el backend (status 0). */
public class ApiException extends RuntimeException {

    private final int status;
    private final List<String> detalles;

    public ApiException(int status, String mensaje, List<String> detalles) {
        super(mensaje);
        this.status = status;
        this.detalles = detalles == null ? List.of() : detalles;
    }

    public int getStatus() {
        return status;
    }

    public boolean esSinConexion() {
        return status == 0;
    }

    /** Mensaje listo para mostrar al usuario, incluyendo los detalles de validacion. */
    public String mensajeCompleto() {
        if (detalles.isEmpty()) return getMessage();
        return getMessage() + ": " + String.join("; ", detalles);
    }
}
