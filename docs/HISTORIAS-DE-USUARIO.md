# Historias de usuario: Sistema de Gestión de Incidentes de TI

Plataforma para que una empresa reporte y gestione problemas tecnológicos: se asignan a técnicos, se priorizan,
se hace seguimiento hasta su solución y se guarda un historial para detectar problemas recurrentes y mejorar los
tiempos de respuesta.

## Roles

Son los actores del diagrama de contexto. En la sala de control se eligen en **"Actuando como"**.

| Rol | Quién es | Rol en el sistema |
|---|---|---|
| **Empleado** | Persona de cualquier área que sufre el problema | `REPORTANTE` |
| **Técnico** | Personal de soporte que atiende los incidentes | `TECNICO` |
| **Administrador** | Coordinador de la mesa de ayuda | `ADMINISTRADOR` |
| **Gerencia de TI** | Responsable de mejorar el servicio; consume los reportes | (consulta reportes) |

**Formato:** *Como* [rol] *quiero* [acción] *para* [beneficio]. Los criterios de aceptación usan *Dado / Cuando / Entonces*.
**Prioridad:** MoSCoW (Debe, Debería, Podría). **Estimación:** puntos de historia (1, 2, 3, 5, 8).

## Resumen

| ID | Historia | Rol | Prioridad | Puntos |
|---|---|---|---|---|
| **Épica 1: Reporte de incidentes** |||||
| HU-01 | Reportar un incidente | Empleado | Debe | 3 |
| HU-02 | Ver si el equipo ya tuvo fallas al reportar | Empleado | Podría | 2 |
| HU-03 | Consultar mi incidente por su código | Empleado | Debe | 2 |
| **Épica 2: Asignación a técnicos** |||||
| HU-04 | Asignar un técnico manualmente | Administrador | Debe | 3 |
| HU-05 | Asignar automáticamente al técnico adecuado | Administrador | Debería | 5 |
| HU-06 | Ver la carga de trabajo de cada técnico | Administrador | Debería | 3 |
| **Épica 3: Prioridades y SLA** |||||
| HU-07 | Definir la prioridad con su plazo de atención | Empleado | Debe | 3 |
| HU-08 | Cambiar la prioridad y recalcular el plazo | Administrador | Debe | 2 |
| HU-09 | Ver qué incidentes están vencidos o por vencer | Técnico | Debería | 2 |
| **Épica 4: Seguimiento hasta la solución** |||||
| HU-10 | Iniciar la atención y ponerla en espera | Técnico | Debe | 3 |
| HU-11 | Registrar la solución aplicada | Técnico | Debe | 2 |
| HU-12 | Cerrar o reabrir mi incidente | Empleado | Debe | 2 |
| HU-13 | Cancelar un incidente | Administrador | Debería | 1 |
| HU-14 | Agregar comentarios de seguimiento | Técnico | Debería | 2 |
| HU-15 | Mover incidentes en un tablero del ciclo de vida | Técnico | Debería | 5 |
| HU-16 | Buscar y filtrar incidentes | Técnico | Debe | 3 |
| **Épica 5: Historial y recurrencias** |||||
| HU-17 | Ver el historial completo de un incidente | Administrador | Debe | 3 |
| HU-18 | Consultar soluciones anteriores del mismo equipo | Técnico | Debería | 2 |
| HU-19 | Identificar problemas recurrentes | Gerencia de TI | Debe | 5 |
| **Épica 6: Reportes y tiempos de respuesta** |||||
| HU-20 | Medir tiempos de respuesta y resolución | Gerencia de TI | Debe | 5 |
| HU-21 | Ver un tablero con el estado general | Administrador | Debería | 3 |
| **Épica 7: Gestión de usuarios** |||||
| HU-22 | Registrar usuarios con su rol | Administrador | Debe | 2 |
| HU-23 | Dar de baja a un usuario sin perder su historial | Administrador | Debería | 1 |
| **Épica 8: Integración y verificación** |||||
| HU-24 | Probar la API con Postman y Swagger | Desarrollador | Debe | 3 |
| HU-25 | Ver las solicitudes y respuestas HTTP entre capas | Evaluador del proyecto | Podría | 3 |
| | | | **Total** | **70** |

---

## Épica 1: Reporte de incidentes

### HU-01: Reportar un incidente
**Como** empleado **quiero** reportar un problema tecnológico con su descripción, categoría, prioridad y equipo
afectado **para** que la mesa de ayuda lo atienda.

- Dado que lleno título, descripción, categoría, prioridad y quién reporta, cuando envío el formulario, entonces el
  sistema crea el incidente en estado **Abierto** con un código consecutivo `INC-000001`.
- El sistema registra la fecha de creación, calcula la fecha límite según la prioridad y guarda el primer evento del historial.
- Dado que falta un dato obligatorio, cuando envío, entonces recibo un error 400 que indica qué campo falta.
- El equipo afectado se normaliza (`pc-0045` pasa a `PC-0045`) para poder agrupar fallas repetidas.

*Implementación:* `POST /api/incidentes` · pantalla **Reportar incidente**.

### HU-02: Ver si el equipo ya tuvo fallas al reportar
**Como** empleado **quiero** saber, mientras reporto, si el equipo ya tuvo incidentes y qué solución se aplicó **para**
dar más contexto o probar esa solución.

- Dado que escribo un equipo con incidentes previos, entonces veo cuántos tiene y la última solución registrada.
- Si el equipo no tiene antecedentes, no se muestra ningún aviso.

*Implementación:* `GET /api/incidentes` · aviso en **Reportar incidente**.

### HU-03: Consultar mi incidente por su código
**Como** empleado **quiero** consultar mi incidente con su código **para** conocer su estado, el técnico asignado y la solución.

- Dado un código válido (`INC-000004`) o el id interno, entonces veo todos sus datos, su estado y los tiempos.
- Dado un código que no existe, entonces recibo un error 404 con el mensaje "Incidente no encontrado".

*Implementación:* `GET /api/incidentes/{id o código}` · **Ficha del incidente**.

## Épica 2: Asignación a técnicos

### HU-04: Asignar un técnico manualmente
**Como** administrador **quiero** asignar o reasignar un incidente a un técnico **para** que alguien responsable lo atienda.

- Dado un incidente abierto, cuando lo asigno a un técnico activo, entonces pasa a **Asignado** y queda registrada la fecha de asignación.
- Dado un incidente que ya está en progreso, cuando lo reasigno, entonces cambia el técnico sin cambiar el estado.
- Dado que elijo a un usuario que no es técnico o está inactivo, entonces la API responde 409 y no se asigna.
- Dado un incidente cerrado o cancelado, entonces no admite reasignación.

*Implementación:* `PATCH /api/incidentes/{id}/asignar` · **Ficha del incidente**, **Ciclo de vida**.

### HU-05: Asignar automáticamente al técnico adecuado
**Como** administrador **quiero** que el sistema elija al técnico **para** repartir la carga sin revisar uno por uno.

- Cuando pido la asignación automática, el sistema prefiere a los técnicos cuya especialidad coincide con la categoría del incidente.
- Entre ellos, elige al que tiene menos incidentes pendientes (asignados, en progreso o en espera).
- Si no hay técnicos activos, la API responde 409 con el mensaje "No hay técnicos activos disponibles".

*Implementación:* `PATCH /api/incidentes/{id}/asignar-automatico` · botón en el **Tablero** ("Esperando técnico").

### HU-06: Ver la carga de trabajo de cada técnico
**Como** administrador **quiero** ver cuántos incidentes pendientes y resueltos tiene cada técnico **para** balancear el trabajo.

- Veo, por técnico, los incidentes asignados, los pendientes, los resueltos, el tiempo promedio de resolución y los que incumplieron el SLA.
- La lista se ordena de mayor a menor carga pendiente.

*Implementación:* `GET /api/reportes/tecnicos` · **Usuarios y técnicos**.

## Épica 3: Prioridades y SLA

### HU-07: Definir la prioridad con su plazo de atención
**Como** empleado **quiero** indicar qué tan urgente es el problema **para** que se atienda en el plazo adecuado.

- Las prioridades y sus plazos de resolución son: Crítica 4 h, Alta 8 h, Media 24 h, Baja 72 h.
- La fecha límite se calcula como fecha de creación más el plazo de la prioridad.
- El formulario explica cuándo usar cada prioridad.

*Implementación:* enumerado `Prioridad`, `Incidente.recalcularSla()` · **Reportar incidente**.

### HU-08: Cambiar la prioridad y recalcular el plazo
**Como** administrador **quiero** cambiar la prioridad de un incidente **para** corregirla cuando el impacto es mayor o menor al reportado.

- Cuando cambio la prioridad, entonces se recalcula la fecha límite y queda un evento "Cambio de prioridad" con el valor anterior y el nuevo.
- Un incidente cerrado o cancelado no admite cambios de prioridad.

*Implementación:* `PATCH /api/incidentes/{id}/prioridad` · **Ficha del incidente**.

### HU-09: Ver qué incidentes están vencidos o por vencer
**Como** técnico **quiero** ver cuánto falta para que venza cada incidente **para** atender primero los más urgentes.

- Cada incidente muestra "Vence en …", "Vencido hace …" o, si ya terminó, "Dentro del SLA" o "SLA incumplido".
- Los que vencen en menos de una hora se resaltan, y los vencidos se muestran en rojo.

*Implementación:* campos `fechaLimiteSla` y `slaIncumplido` · tarjetas del **Ciclo de vida** y **Ficha**.

## Épica 4: Seguimiento hasta la solución

### HU-10: Iniciar la atención y ponerla en espera
**Como** técnico **quiero** marcar cuándo empiezo a trabajar en un incidente y cuándo queda en espera **para** que todos
sepan en qué punto está.

- El ciclo de vida es Abierto → Asignado → En progreso ⇄ En espera → Resuelto → Cerrado. Desde cualquier estado no final se puede cancelar.
- La primera vez que pasa a **En progreso** se registra la fecha de primera respuesta.
- Dado un cambio no permitido (por ejemplo, de Abierto a Cerrado), la API responde 409 e indica los pasos permitidos.
- Cada cambio guarda quién lo hizo y un motivo opcional.

*Implementación:* `PATCH /api/incidentes/{id}/estado`, enumerado `EstadoIncidente` · **Ficha** (Siguiente paso).

### HU-11: Registrar la solución aplicada
**Como** técnico **quiero** registrar la solución al resolver un incidente **para** que quede documentada y sirva en futuras fallas.

- No se puede pasar a **Resuelto** sin escribir la solución.
- Al resolver se registra la fecha de resolución y un evento "Solución registrada".

*Implementación:* `PATCH /api/incidentes/{id}/resolver` · diálogo "Resolver" en **Ciclo de vida** y **Ficha**.

### HU-12: Cerrar o reabrir mi incidente
**Como** empleado **quiero** confirmar que el problema quedó solucionado o reabrirlo si sigue ocurriendo **para** que el
incidente no se cierre sin estar resuelto.

- Desde **Resuelto** puedo cerrarlo (queda la fecha de cierre) o reabrirlo (vuelve a En progreso).
- Al reabrir, se borran la solución y la fecha de resolución actuales; la solución anterior queda en el historial.

*Implementación:* `PATCH /api/incidentes/{id}/estado` · **Ficha del incidente**.

### HU-13: Cancelar un incidente
**Como** administrador **quiero** cancelar incidentes duplicados o que no proceden **para** mantener limpia la cola.

- Se puede cancelar desde cualquier estado que no sea final, con un motivo.
- Solo se pueden eliminar físicamente los incidentes abiertos o cancelados; el resto se cancela.

*Implementación:* `PATCH /estado` con `CANCELADO`, `DELETE /api/incidentes/{id}` · **Ficha**, columna "Cancelado".

### HU-14: Agregar comentarios de seguimiento
**Como** técnico **quiero** dejar comentarios con los avances **para** que el empleado y el equipo conozcan el progreso.

- Cada comentario guarda su autor y su fecha, y también queda como evento en el historial.
- Los comentarios se muestran del más reciente al más antiguo.

*Implementación:* `POST /api/incidentes/{id}/comentarios` · **Ficha del incidente**.

### HU-15: Mover incidentes en un tablero del ciclo de vida
**Como** técnico **quiero** ver los incidentes en columnas por estado y moverlos arrastrándolos **para** gestionar mi trabajo de forma visual.

- Hay una columna por estado con la cantidad de incidentes; cada tarjeta muestra el código, la prioridad, el técnico y el SLA.
- Al arrastrar una tarjeta se resaltan las columnas permitidas y se atenúan las demás.
- Al soltarla en Asignado se pide el técnico, y en Resuelto se pide la solución.
- Cada tarjeta tiene un menú "Mover a…" como alternativa al arrastre.

*Implementación:* pantalla **Ciclo de vida** (usa `/estado`, `/asignar` y `/resolver`).

### HU-16: Buscar y filtrar incidentes
**Como** técnico **quiero** filtrar los incidentes por estado, prioridad, categoría, técnico, texto o equipo **para**
encontrar rápido los que me interesan.

- Los filtros se pueden combinar, y la lista se ordena del más reciente al más antiguo.
- Un valor de filtro inválido devuelve un error 400.

*Implementación:* `GET /api/incidentes?estado=&prioridad=&categoria=&tecnicoId=&reportadoPorId=&texto=` · filtros del **Ciclo de vida**.

## Épica 5: Historial y recurrencias

### HU-17: Ver el historial completo de un incidente
**Como** administrador **quiero** ver todos los cambios de un incidente con fecha y responsable **para** auditar cómo se atendió.

- Se registran la creación, las asignaciones, los cambios de estado y de prioridad, los comentarios, la solución y las actualizaciones de datos.
- Cada evento guarda la fecha, el usuario, la descripción y los valores anterior y nuevo.
- La ficha lo muestra como una línea de tiempo.

*Implementación:* `GET /api/incidentes/{id}/historial` (historial embebido en MongoDB) · **Ficha** (Historial).

### HU-18: Consultar soluciones anteriores del mismo equipo
**Como** técnico **quiero** ver los incidentes ya resueltos del mismo equipo **para** reutilizar soluciones que funcionaron.

- Veo los incidentes anteriores del mismo activo que tienen solución, del más reciente al más antiguo, excluyendo el actual.

*Implementación:* `GET /api/incidentes/{id}/antecedentes` · **Ficha** (Antecedentes del activo).

### HU-19: Identificar problemas recurrentes
**Como** gerencia de TI **quiero** saber qué equipos fallan una y otra vez **para** decidir soluciones de fondo (reemplazo o mantenimiento).

- Un problema es recurrente cuando un mismo equipo tiene N o más incidentes de la misma categoría en los últimos D días (por defecto, 3 en 30 días).
- Puedo cambiar el periodo y el umbral.
- Para cada problema veo el equipo, la categoría, la cantidad, la última falla y los códigos de los incidentes.
- El tablero muestra un mapa de equipos por día y resalta los que la API marca como recurrentes.

*Implementación:* `GET /api/reportes/problemas-recurrentes?dias=&minimo=` (agregación en MongoDB) · **Tablero**, **Recurrencias y tiempos**.

## Épica 6: Reportes y tiempos de respuesta

### HU-20: Medir tiempos de respuesta y resolución
**Como** gerencia de TI **quiero** conocer los tiempos promedio de respuesta y resolución por prioridad **para** saber si
cumplimos los plazos y dónde mejorar.

- Por prioridad veo el total, los resueltos, el promedio de primera respuesta, el promedio de resolución, la meta de SLA, los incumplidos y el porcentaje de cumplimiento.
- Puedo elegir el periodo (7, 15, 30, 60 o 90 días).

*Implementación:* `GET /api/reportes/tiempos?dias=` · **Tablero** (Cumplimiento del SLA), **Recurrencias y tiempos**.

### HU-21: Ver un tablero con el estado general
**Como** administrador **quiero** un tablero con la situación de la mesa de ayuda **para** decidir qué atender primero.

- Muestra el total de incidentes, los pendientes y los que tienen el SLA incumplido, además de la distribución por estado.
- Lista los incidentes que esperan técnico, ordenados por prioridad.
- Muestra los últimos movimientos del historial.

*Implementación:* `GET /api/reportes/resumen` y `GET /api/incidentes` · **Tablero**.

## Épica 7: Gestión de usuarios

### HU-22: Registrar usuarios con su rol
**Como** administrador **quiero** registrar empleados, técnicos y administradores **para** que puedan usar el sistema.

- Cada usuario tiene nombre, email único, rol y departamento; los técnicos tienen además una especialidad.
- Un email repetido devuelve un error 409, y un email con formato inválido devuelve un error 400.

*Implementación:* `POST /api/usuarios`, `PUT /api/usuarios/{id}` · **Usuarios y técnicos**.

### HU-23: Dar de baja a un usuario sin perder su historial
**Como** administrador **quiero** desactivar usuarios que ya no están **para** que no reciban asignaciones sin borrar su historial.

- La baja es lógica: el usuario queda inactivo pero sigue apareciendo en el historial.
- Un usuario inactivo no puede realizar operaciones ni recibir asignaciones.

*Implementación:* `DELETE /api/usuarios/{id}` · **Usuarios y técnicos**.

## Épica 8: Integración y verificación

### HU-24: Probar la API con Postman y Swagger
**Como** desarrollador **quiero** probar todos los endpoints con Postman y con la documentación interactiva **para**
verificar las solicitudes y respuestas HTTP.

- La colección de Postman cubre usuarios, el ciclo de vida completo, los reportes y los casos de error (400, 404 y 409).
- La colección guarda sola los ids y códigos creados, y se puede ejecutar varias veces.
- Swagger UI está disponible en `/swagger-ui.html`.
- Las pruebas automáticas (`mvn test`) recorren todos los endpoints contra un MongoDB embebido.

*Implementación:* `postman/Incidentes-TI.postman_collection.json`, springdoc-openapi, `IncidentesApiIntegrationTest`.

### HU-25: Ver las solicitudes y respuestas HTTP entre capas
**Como** evaluador del proyecto **quiero** ver cómo viaja cada solicitud entre el navegador, el frontend, la API y
MongoDB **para** comprobar la arquitectura descrita en los diagramas.

- La pantalla muestra las capas del diagrama de despliegue y el estado de conexión de la API y de MongoDB.
- Se pueden enviar solicitudes de ejemplo, incluidos errores 400 y 404.
- Una bitácora en vivo muestra el método, la ruta, el código de respuesta y la duración, además del cuerpo de cada solicitud y respuesta.

*Implementación:* `RegistroHttp`, `ApiCliente` · pantalla **Arquitectura en vivo** (frontend).
