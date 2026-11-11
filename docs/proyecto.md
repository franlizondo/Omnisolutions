# Documento del proyecto — Omni Solutions Beta

## 1. Antecedentes

### 1.1 TP1: el problema y la idea

- **Problema.** A las personas les cuesta encontrar a alguien que resuelva un inconveniente puntual,
  muchas veces sin saber siquiera a quién recurrir. Además, quien quiere ofrecer sus servicios no tiene
  un lugar donde hacerlo.
- **A quién afecta.** A cualquier persona, sin importar el rubro ni la magnitud del problema.
- **Cómo se resuelve hoy.** Con redes sociales, avisos en diarios, propagandas y recomendaciones informales.
- **Por qué una solución digital.** Reúne en un solo espacio, simple e intuitivo, lo que hoy está disperso
  en varios medios.
- **Propuesta: Omni Solutions.** Una aplicación que funciona como intermediaria entre **empleadores**
  y **empleados**, con un listado de trabajos y servicios. Permite ofrecer trabajos, ofrecer servicios y
  encontrar personas con necesidades y habilidades específicas.
- **Metodología.** El equipo eligió **Lean Startup**: lanzar un MVP para validar la demanda con usuarios
  reales en un ciclo construir–medir–aprender. Para definir la idea usó etapas de **Design Thinking**
  (empatizar, definir, idear y bocetar).
- **Ideas que quedaron para más adelante.** Métodos de pago, niveles de urgencia, número de emergencia y
  denuncias entre usuarios.

### 1.2 TP2: épicas, historias de usuario y backlog

El MVP se dividió en **6 épicas** y **17 historias de usuario** (78 story points estimados con
Planning Poker y priorizados con MoSCoW):

| Épica | Tamaño | Historias |
|---|---|---|
| EP1 Autenticación y roles | M | Registro con correo y contraseña · login con Google/Apple · elección de rol |
| EP2 Gestión de ofertas y demandas | L | Publicar servicio (empleado) · publicar trabajo (empleador) · foto y biografía en el perfil |
| EP3 Búsqueda y filtrado | S | Búsqueda por palabra clave · filtro por categoría · filtro por urgencia |
| EP4 Solicitudes y contratación | M | Solicitud directa del empleador · aceptar o rechazar solicitudes · chat interno |
| EP5 Sistema de pagos | XL | Mercado Pago · guardar tarjeta · pago manual / en efectivo |
| EP6 Seguridad y soporte | S | Reportar mala conducta · botón de emergencia |

El **Sprint 1** quedó formado por las 9 historias *Must Have*. Las historias que más discusión generaron
al estimar fueron el chat interno (entre 3 y 13 puntos), la integración con Mercado Pago (entre 8 y 21)
y el botón de emergencia (entre 2 y 8), por dudas sobre su alcance y por falta de experiencia del equipo.

## 2. Del backlog del TP2 a lo construido

Durante el desarrollo se mantuvo el objetivo del MVP (conectar empleadores y empleados), pero se
**simplificó el flujo de contratación**: en lugar de que el empleador envíe solicitudes y de un chat
interno, **el empleado se postula a un trabajo** y, cuando el empleador **acepta** la postulación, ambos
pueden ver el teléfono del otro. Así se cubre el flujo completo de punta a punta sin la complejidad del
chat en tiempo real, que fue una de las historias más difíciles de estimar.

| HU | Historia (TP2) | Prioridad | Estado en la beta |
|---|---|---|---|
| EP1-HU1 | Registro con correo y contraseña | Must | ✅ Hecho con **nombre de usuario** en lugar de correo; usuario duplicado → error |
| EP1-HU2 | Login con Google/Apple | Should | ❌ Pendiente |
| EP1-HU3 | Elegir rol (empleador / empleado) | Must | ✅ El rol se elige al registrarse. ❌ No se puede cambiar después (decisión de diseño) |
| EP2-HU1 | El empleado publica un servicio | Must | ✅ Hecho con validaciones (título obligatorio, longitudes). ❌ Sin categorías |
| EP2-HU2 | El empleador publica un trabajo | Must | ✅ Aparece en el feed público. ❌ Sin notificación de confirmación |
| EP2-HU3 | Foto y biografía en el perfil | Could | ❌ Pendiente (el perfil permite editar nombre y teléfono) |
| EP3-HU1 | Búsqueda por palabra clave | Must | ✅ Busca en título, descripción y ubicación; muestra un mensaje si no hay resultados |
| EP3-HU2 | Filtro por categorías | Must | 🟡 Parcial: filtro por tipo (Trabajos / Servicios), sin categorías |
| EP3-HU3 | Filtro por urgencia | Should | ❌ Pendiente |
| EP4-HU1 | Solicitud directa del empleador | Must | 🔄 Reemplazada por la **postulación del empleado** a un trabajo |
| EP4-HU2 | Aceptar o rechazar | Must | ✅ El empleador acepta o rechaza cada postulación; la aceptación habilita el contacto |
| EP4-HU3 | Chat interno | Should | 🔄 Reemplazado por el intercambio de teléfonos tras la aceptación |
| EP5-HU1 | Pago con Mercado Pago | Could | ❌ Pendiente |
| EP5-HU2 | Guardar tarjeta | Should | ❌ Pendiente |
| EP5-HU3 | Pago manual / en efectivo | Must | ❌ Pendiente (el pago se acuerda por fuera de la aplicación) |
| EP6-HU1 | Reportar mala conducta | Must | ❌ Pendiente |
| EP6-HU2 | Botón de emergencia | Could | ❌ Pendiente |

**Resumen:** de las 9 historias del Sprint 1, 6 se completaron (algunas con ajustes), 1 quedó parcial
(EP3-HU2, filtro por categorías) y 2 quedaron pendientes (EP5-HU3, pago manual, y EP6-HU1, reportes).
Las épicas de pagos (EP5) y de seguridad y soporte (EP6) quedaron fuera de la beta.

## 3. Objetivo y alcance

Primera versión beta funcional de una plataforma de intermediación laboral que permite completar el flujo:
registro → inicio de sesión → consulta de publicaciones → creación de trabajos/servicios según el rol → detalle →
postulación → consulta de postulaciones → aceptación/rechazo → acceso al teléfono con postulación aceptada →
modificación del perfil y administración de publicaciones propias.

Todo el flujo funciona contra MySQL a través del backend; no hay datos simulados en el frontend.

## 4. Arquitectura

Aplicación monolítica Spring Boot en capas, con el frontend estático servido por el mismo servidor.

```
Navegador (HTML + CSS + JS)  ──fetch JSON + cookie de sesión + token CSRF──▶  Spring Security
                                                                                    │
                                    controller  (endpoints REST, validación @Valid) │
                                         │                                           ▼
                                    service     (reglas de negocio, permisos, contacto)
                                         │
                                    repository  (Spring Data JPA)
                                         │
                                    MySQL 8  (usuarios, publicaciones, postulaciones)
```

| Paquete | Contenido |
|---|---|
| `model` | Entidades JPA `Usuario`, `Publicacion`, `Postulacion` y enums de tipos y estados |
| `repository` | Interfaces Spring Data con consultas JPQL (feed, propias, recibidas, realizadas) |
| `service` | `UsuarioService` (registro, perfil, usuario actual, `UserDetailsService`), `PublicacionService`, `PostulacionService`, `ApiException` |
| `controller` | `AuthController`, `UsuarioController`, `PublicacionController`, `PostulacionController`, `GlobalExceptionHandler` |
| `dto` | Records de entrada (con validaciones) y de salida (sin datos privados) |
| `config` | `SecurityConfig` (sesión, CSRF, BCrypt, respuestas 401/403 JSON, CSP) |

Frontend: 7 páginas HTML, `css/styles.css` y un JS por página más `js/app.js` con lo compartido
(cliente de API con CSRF, barra superior, formato, filas de postulaciones, acción de postularse y contacto).
No se usan frameworks.

## 5. Reglas de negocio implementadas

Todas se validan en el backend (capa de servicio); el frontend solo oculta acciones que no corresponden.

**Usuarios**
- Usuario único (sin distinguir mayúsculas). Contraseña con BCrypt. Rol `EMPLEADOR` o `EMPLEADO`, no modificable.
- El perfil permite cambiar nombre y teléfono. El teléfono es opcional.

**Publicaciones**
- Empleador → `TRABAJO`; empleado → `SERVICIO`. Si se envía un tipo que no corresponde al rol: 403.
- Estado inicial `ACTIVA`, fecha automática. Solo el autor puede modificar, cerrar o eliminar (403 en otro caso).
- El feed muestra solo publicaciones `ACTIVA`.

**Postulaciones**
- Solo empleados (403), solo a publicaciones de tipo trabajo (400), solo si están activas (409), sin duplicados (409,
  respaldado por la restricción UNIQUE de la base).
- Solo el empleador propietario del trabajo acepta o rechaza (403). Solo se resuelven postulaciones `PENDIENTE` (409).
- Cada postulación es independiente: aceptar una no modifica las demás; puede haber varias aceptadas por trabajo.

**Contacto**
- `GET /api/postulaciones/{id}/contacto` entrega el teléfono de la contraparte solo a las dos partes de una
  postulación `ACEPTADA`. Antes de la aceptación o para terceros: 403. Si no hay teléfono, `telefonoDisponible=false`
  y un mensaje.
- Ningún otro endpoint devuelve teléfonos de terceros (verificado por prueba automatizada). El único otro lugar donde
  aparece un teléfono es `GET /api/usuarios/me`, que devuelve el propio.

## 6. Seguridad

- **Autenticación por sesión** (cookie `JSESSIONID`, HttpOnly, SameSite=Lax), cambio de id de sesión al iniciar sesión.
- **CSRF** activo con cookie `XSRF-TOKEN` y encabezado `X-XSRF-TOKEN` en toda operación que modifica datos.
- **Contraseñas** con BCrypt (longitud máxima 72 bytes validada).
- **Endpoints protegidos**: todo `/api/**` salvo `/api/auth/**` requiere sesión (401 JSON). Las páginas HTML internas
  redirigen a `index.html` sin sesión.
- **Control de rol y de propiedad** en los servicios; los DTO de salida nunca exponen contraseñas, usuarios de terceros
  ni teléfonos.
- **Validación de entradas** con Bean Validation (longitudes, formato de usuario y teléfono, rol obligatorio) y manejo
  centralizado de errores (`GlobalExceptionHandler`), sin trazas internas en las respuestas.
- **XSS**: todo contenido de usuario se escapa en el frontend y se envía una Content-Security-Policy que solo permite
  scripts y estilos propios.
- **Credenciales** fuera del código: variables de entorno o `.env` (excluido de Git).

## 7. Decisiones tomadas (información secundaria no especificada)

| Tema | Decisión |
|---|---|
| Versión de Spring Boot | 3.5.16 (rama 3.x, ampliamente documentada y compatible con NetBeans y Java 21) |
| Creación del esquema | `schema.sql` idempotente al iniciar + `ddl-auto=validate`; además, script manual en `database/` |
| Tipos/estados en MySQL | `VARCHAR` + `CHECK` en lugar de `ENUM` |
| Nombre de usuario | Se guarda en minúsculas; el login no distingue mayúsculas |
| Inicio tras registro | El frontend inicia sesión automáticamente después de registrar |
| Detalle de publicaciones cerradas | Visible para usuarios autenticados por enlace directo (p. ej. un postulante), pero no aparece en el feed |
| Postulaciones pendientes de un trabajo cerrado | El empleador todavía puede aceptarlas o rechazarlas; cerrar solo impide nuevas postulaciones |
| Eliminar publicación | Elimina también sus postulaciones (con confirmación en pantalla) |
| Datos del postulante visibles al empleador | Solo el nombre (no usuario ni teléfono hasta aceptar) |
| Búsqueda | Filtro por tipo y texto en título, descripción y ubicación sobre publicaciones activas |
| Formato de teléfono | 6 a 20 caracteres: dígitos, espacios, `+`, `-` y paréntesis |
| Fechas | `DATETIME` en la hora local del servidor, sin fracciones de segundo |
| Pruebas automatizadas | H2 en modo MySQL para que `mvn test` funcione sin servidor; misma suite validada también contra MySQL |

## 8. Archivos del proyecto

Ver la sección *Estructura* del [README](../README.md). Archivos principales creados:
`pom.xml`, `.env.example`, `.gitignore`, `database/01-crear-base-datos.sql`, `database/02-datos-prueba.sql`,
`src/main/resources/{application.properties, schema.sql}`, 24 clases Java en `src/main/java`, 7 páginas HTML,
1 hoja de estilos y 8 scripts JS en `static/`, `favicon.svg`, la suite `OmniSolutionsApiTest` y el script
`src/test/e2e/e2e-navegador.js`.

## 9. Pruebas

### 9.1 Pruebas automatizadas de integración (`mvn test`)

`OmniSolutionsApiTest`: 20 pruebas con `@SpringBootTest` + MockMvc que pasan por la cadena real de Spring Security
(sesión obtenida mediante el login real, CSRF), los servicios y JPA.

| # | Prueba | Verifica |
|---|---|---|
| 1 | `registroExitosoGuardaContrasenaConBCrypt` | Registro 201, hash `$2a$`, la respuesta no incluye contraseña |
| 2 | `registroConUsuarioDuplicadoDevuelve409` | Usuario duplicado (también con otra capitalización) |
| 3 | `registroConDatosInvalidosDevuelve400` | Validaciones por campo y rol inexistente |
| 4 | `inicioDeSesionCorrectoEIncorrecto` | Login 200 / contraseña errónea 401 / usuario inexistente 401 |
| 5 | `cierreDeSesionInvalidaLaSesion` | Logout y 401 posterior |
| 6 | `modificarPerfilNoPermiteCambiarElRol` | Cambio de nombre y teléfono; el rol enviado se ignora; teléfono vacío y formato inválido |
| 7 | `publicacionSegunRol` | Empleador→TRABAJO, empleado→SERVICIO; tipo cruzado → 403 |
| 8 | `publicacionConDatosInvalidosDevuelve400` | Validación de título y descripción |
| 9 | `modificarCerrarYEliminarPublicacionPropia` | Modificar, listar propias, cerrar y eliminar |
| 10 | `noSePuedenAdministrarPublicacionesAjenas` | Modificar/cerrar/eliminar ajena → 403 y sin cambios |
| 11 | `publicacionesCerradasNoAparecenEnElFeed` | Feed, filtros por tipo y texto, cerradas excluidas |
| 12 | `postulacionValidaYDuplicada` | Postulación 201, duplicada 409, conteos y estado propio |
| 13 | `postulacionAServicioTrabajoCerradoOPorEmpleadorSeRechaza` | Servicio 400, empleador 403, trabajo cerrado 409, inexistente 404 |
| 14 | `variasPostulacionesAceptadasYRechazoIndependientes` | Dos aceptadas y una rechazada en el mismo trabajo; aceptar no altera las demás; resueltas no cambian (409) |
| 15 | `soloElEmpleadorPropietarioGestionaPostulaciones` | Otro empleador o un empleado no pueden resolver ni ver recibidas ajenas |
| 16 | `telefonoSoloDisponibleTrasLaAceptacion` | 403 antes de aceptar; después cada parte ve el de la otra; tercero 403 |
| 17 | `telefonoNoProporcionadoMuestraMensaje` | Mensaje cuando no hay teléfono |
| 18 | `telefonosNoSeExponenEnListadosNiDetalles` | Feed, detalle, propias, realizadas y recibidas sin teléfonos |
| 19 | `endpointsProtegidosSinSesion` | 401 en la API sin sesión; redirección de páginas internas |
| 20 | `operacionesSinTokenCsrfSonRechazadas` | POST sin CSRF → 403 |

**Resultados obtenidos (ejecutados el 04/11/2026):**
- Con H2 (configuración por defecto de `mvn test`): **20 ejecutadas, 0 fallas, 0 errores**.
- Con **MySQL 8.0.46** (base `omni_solutions_test`, esquema creado por `schema.sql` y validado por Hibernate):
  **20 ejecutadas, 0 fallas, 0 errores**.

### 9.2 Prueba de punta a punta en navegador

`src/test/e2e/e2e-navegador.js` (Chromium + Playwright) recorre la interfaz real con la aplicación conectada a
MySQL 8, con un empleador y dos empleados: protección de páginas, login incorrecto, registro, publicación y edición,
postulación desde el feed y desde el detalle, ausencia de "Postularme" en servicios, dos aceptaciones y un rechazo
independientes, teléfono visible para el empleador, mensaje de teléfono no proporcionado, cierre desde Mis
publicaciones, actualización de teléfono en el perfil, exclusión del trabajo cerrado del feed y del botón de
postulación, teléfono del empleador visible para el empleado y ausencia de scroll horizontal en 390 px de ancho.

**Resultado: 22 verificaciones correctas, sin errores de JavaScript ni violaciones de CSP.**
(Las únicas entradas de consola son respuestas 401 esperadas: la consulta de sesión de la pantalla de acceso y el
intento de login incorrecto.)

### 9.3 Verificaciones manuales sobre MySQL

- Inserción directa de una postulación duplicada → `ERROR 1062` por `uk_postulaciones_usuario_publicacion`.
- Postulación con usuario inexistente → `ERROR 1452` (clave foránea).
- Usuario con rol `ADMIN` → `ERROR 3819` (`ck_usuarios_tipo`).
- `01-crear-base-datos.sql` y `02-datos-prueba.sql` ejecutados dos veces seguidas sin errores; login con
  `lucia.fernandez` / `Omni2026` y `panaderia.sol` / `Omni2026` correcto, acentos preservados.

## 10. Problemas pendientes y limitaciones conocidas

- **Servicios sin canal de contacto**: según las reglas, solo los empleados se postulan y solo a trabajos; por lo tanto
  un empleador interesado en un servicio no tiene forma de contactar al empleado dentro de la plataforma.
  Requiere una decisión funcional (por ejemplo, "solicitudes de servicio").
- Las sesiones se guardan en memoria: se pierden al reiniciar el servidor.
- No hay límite de intentos de inicio de sesión.
- No hay paginación del feed (adecuado para el volumen de una beta).
- El DDL está en dos archivos (`schema.sql` y `database/01-crear-base-datos.sql`) que deben mantenerse iguales.
- Para producción falta HTTPS (y cookie `Secure`), registro de auditoría y una herramienta de migraciones
  (Flyway/Liquibase) si el esquema evoluciona.
- El script E2E necesita Node.js + Playwright y una base sin los usuarios `laura`, `martin` y `sofia`.
- Spring Boot 3.5 es una rama estable anterior a 4.x; conviene planificar la actualización.

## 11. Funcionalidades no implementadas (fuera del MVP)

- Cambio y recuperación de contraseña, baja de cuenta.
- Reapertura de publicaciones cerradas.
- Retirar una postulación por parte del empleado.
- Notificaciones (correo o en la aplicación), mensajería interna.
- Fotos de perfil, adjuntos o CV.
- Panel de administración y moderación de contenido.

## 12. Conclusiones

> **A completar por el grupo**, como pide el TP5: qué aprendieron, qué les costó y qué harían distinto si
> empezaran de nuevo.
