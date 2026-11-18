# Sala de control: frontend del Sistema de Gestión de Incidentes de TI

Interfaz web interactiva escrita **100 % en Java** con **Vaadin 24 + Spring Boot 3.5**.
No guarda datos propios: todo lo lee y lo escribe en la **API REST del backend**
([Backend-Gestion-de-incidentes](https://github.com/Hurtado43/Backend-Gestion-de-incidentes))
mediante **solicitudes HTTP con JSON**, y la API los guarda en **MongoDB**.
Las historias de usuario que implementa están en [`docs/HISTORIAS-DE-USUARIO.md`](docs/HISTORIAS-DE-USUARIO.md).

```
Navegador  ──>  Frontend Vaadin (este proyecto, :8081)  ──HTTP/JSON──>  API Spring Boot (:8080)  ──TCP 27017──>  MongoDB
```

## Pantallas

El menú está organizado como los componentes del diagrama de componentes:

| Menú | Pantalla | Qué hace | Endpoints que usa |
|---|---|---|---|
| Gestión de incidentes | **Tablero** | Mapa de los activos que más fallan (días × activo), estado de la cola, cumplimiento del SLA por prioridad, incidentes que esperan técnico (con asignación automática) y últimos movimientos | `GET /api/incidentes`, `/api/reportes/*`, `PATCH /api/incidentes/{id}/asignar-automatico` |
| | **Ciclo de vida** | Tablero con una columna por estado. Las tarjetas se mueven arrastrándolas o con su menú; solo se permiten los pasos de la máquina de estados (las columnas no alcanzables se atenúan al arrastrar). Resolver pide la solución; asignar pide el técnico | `PATCH /estado`, `/asignar`, `/resolver` |
| | **Ficha del incidente** | Datos, siguiente paso, prioridad (recalcula el SLA), reasignación, comentarios, línea de tiempo del historial y antecedentes del mismo activo | `GET /api/incidentes/{id}`, `/historial`, `/antecedentes`, `POST /comentarios`, `PATCH /prioridad` |
| | **Reportar incidente** | Formulario; al escribir el activo avisa si ya tuvo fallas y muestra la última solución | `POST /api/incidentes` |
| Gestión de usuarios | **Usuarios y técnicos** | Carga de trabajo por técnico, registro y baja lógica de usuarios | `GET/POST/DELETE /api/usuarios`, `GET /api/reportes/tecnicos` |
| Reportes e indicadores | **Recurrencias y tiempos** | Problemas recurrentes con periodo y umbral configurables, tiempos por prioridad y distribución por categoría | `GET /api/reportes/problemas-recurrentes`, `/tiempos`, `/resumen` |
| Para el profesor | **Arquitectura en vivo** | Las capas del diagrama de despliegue con su estado, una consola para lanzar solicitudes de ejemplo (incluidos errores 400 y 404) y la **bitácora HTTP en vivo** con el cuerpo de cada solicitud y respuesta | todas |
| | **Diseño (5 vistas)** | Los diagramas de contexto, funcional, lógico, desarrollo y despliegue, con enlaces a la pantalla donde se ve cada uno | — |

En la barra superior, **"Actuando como"** elige el usuario (empleado, técnico o administrador: los actores del diagrama de
contexto). Su id se envía como `usuarioId` en cada operación y queda en el historial del incidente.

## Cómo ejecutarlo

Requisitos: JDK 21 y Maven 3.9. **Primero** debe estar corriendo el backend con MongoDB (ver su README).

```bash
mvn spring-boot:run
```

Abrir <http://localhost:8081>. La primera ejecución tarda más porque Vaadin prepara el frontend.

Variables opcionales:

| Variable | Por defecto | Uso |
|---|---|---|
| `API_URL` | `http://localhost:8080` | URL de la API REST del backend |
| `PORT` | `8081` | Puerto del frontend |

Ejemplo con la API en otro puerto: `mvn spring-boot:run -Dspring-boot.run.arguments=--api.url=http://localhost:8085`

### Versión de producción (un solo .jar)

```bash
mvn -Pproduction package
java -jar target/incidentes-frontend-1.0.0.jar
```

Compilar con `-Pproduction` necesita Node.js la primera vez (Vaadin lo descarga si no está instalado). Ejecutar el `.jar` no lo necesita.

## Estructura del código

```
com.empresa.salacontrol
├── SalaControlApplication   arranque (Spring Boot) y tema "salacontrol"
├── api        ApiCliente (RestClient con un método por endpoint), Dto (records espejo del modelo),
│              ApiException (errores 400/404/409 o sin conexión), RegistroHttp (bitácora de solicitudes)
├── sesion     SesionUsuario (actor con el que se actúa en la sesión)
├── ui         Formato (textos en español), Piezas (etiquetas de estado/prioridad/SLA),
│              Acciones (cambios de estado con sus reglas), MapaRecurrencias
└── vistas     MainLayout y una clase por pantalla
src/main/frontend/themes/salacontrol   estilos (styles.css y piezas.css)
```
