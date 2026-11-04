# Manual de usuario — Omni Solutions

Omni Solutions conecta a **empleadores** (publican trabajos y contratan) con **empleados** (se postulan a trabajos
y ofrecen sus servicios). El teléfono de cada persona es privado y solo se comparte cuando un empleador **acepta** una
postulación.

## Roles

| Acción | Empleador | Empleado |
|---|:---:|:---:|
| Publicar | Trabajos | Servicios |
| Postularse a trabajos | — | ✔ |
| Ver postulaciones | Recibidas en sus trabajos | Realizadas |
| Aceptar / rechazar postulaciones | ✔ (solo de sus trabajos) | — |
| Ver teléfono de la otra parte | De empleados aceptados | Del empleador que lo aceptó |

El rol se elige al crear la cuenta y **no puede cambiarse**.

## 1. Acceso (`index.html`)

- **Crear cuenta**: completar nombre, usuario (letras, números, `.`, `-`, `_`), contraseña (mínimo 6 caracteres),
  rol y, opcionalmente, teléfono. Al crearla se inicia sesión automáticamente.
- **Iniciar sesión**: usuario y contraseña. El usuario no distingue mayúsculas de minúsculas.
- Si ya hay una sesión abierta, la pantalla lleva directamente al inicio.

## 2. Barra superior

Presente en todas las pantallas internas: **Inicio**, **Publicar**, **Mis publicaciones**, **Postulaciones**,
**Perfil** y **Salir** (cierra la sesión). El buscador busca en las publicaciones activas.
En pantallas pequeñas se muestran solo los íconos.

## 3. Inicio (`inicio.html`)

- **Izquierda**: tu nombre, usuario, rol y un resumen (publicaciones activas, postulaciones pendientes y aceptadas).
  "Ver mi perfil" lleva al perfil.
- **Centro**: el feed de publicaciones **activas** (las cerradas no aparecen).
  - Filtros *Todas / Trabajos / Servicios* y búsqueda por título, descripción o ubicación.
  - Cada tarjeta muestra autor, tipo, fecha, título, descripción resumida y ubicación.
  - **Ver detalle** abre la publicación completa.
  - Empleados: **Postularme** en los trabajos activos. Si ya te postulaste, se muestra el estado.
  - En tus propias publicaciones se indica "Tu publicación" y, en trabajos, la cantidad de postulaciones.
- **Derecha**: información de la plataforma, consejos según tu rol y accesos rápidos.

## 4. Publicar (`publicar.html`)

El tipo se asigna según el rol (empleador → **Trabajo**, empleado → **Servicio**).
Completar título (5–120 caracteres), descripción (10–2000) y ubicación (opcional) y presionar **Publicar**.
La fecha se registra automáticamente y la publicación queda **Activa**.

La misma pantalla se usa para **editar** (botón *Editar* en el detalle o en Mis publicaciones).
Solo se pueden editar publicaciones propias.

## 5. Detalle (`detalle.html`)

Muestra la publicación completa: autor, tipo, ubicación, fecha, estado y descripción. Según quién la consulta:

- **Propietario**: *Editar*, *Cerrar publicación* (deja de aparecer en el feed y no admite nuevas postulaciones) y
  *Eliminar* (borra también sus postulaciones; pide confirmación). Si es un trabajo, debajo aparece la lista de
  **postulaciones recibidas** con *Aceptar*, *Rechazar* y *Ver teléfono*.
- **Empleado** en un trabajo activo: *Postularme a este trabajo*. Si ya se postuló, ve el estado; si fue aceptado,
  *Ver teléfono del empleador*.
- Trabajo cerrado: se informa que no recibe nuevas postulaciones.

## 6. Mis publicaciones (`mis-publicaciones.html`)

Lista de todas tus publicaciones (activas y cerradas) con filtros por estado. Para cada una: *Ver*, *Editar*,
*Cerrar* (si está activa) y *Eliminar*. En los trabajos, el enlace de cantidad de postulaciones lleva a
Postulaciones filtradas por ese trabajo.

## 7. Postulaciones (`postulaciones.html`)

**Empleador — Postulaciones recibidas**
- Filtro por trabajo y por estado (pendientes, aceptadas, rechazadas).
- **Aceptar** o **Rechazar** cada postulación pendiente (se pide confirmación). Cada postulación se gestiona por
  separado: aceptar una **no cambia** las demás y se pueden aceptar varias para el mismo trabajo.
- Una postulación aceptada o rechazada **no puede volver a cambiarse**.
- En las aceptadas, **Ver teléfono** muestra el número del empleado o el aviso "no proporcionó un número de teléfono".

**Empleado — Mis postulaciones**
- Estado de cada postulación (Pendiente, Aceptada, Rechazada) y si el trabajo fue cerrado.
- En las aceptadas, **Ver teléfono del empleador**.

## 8. Perfil (`perfil.html`)

Muestra nombre, usuario, rol, teléfono y fecha de registro. Se pueden modificar el **nombre** y el **teléfono**
(dejarlo vacío para no compartir ningún número). El usuario y el rol no se pueden cambiar.

## Mensajes frecuentes

| Mensaje | Motivo |
|---|---|
| Usuario o contraseña incorrectos | Datos de acceso inválidos |
| El nombre de usuario ya está en uso | Elegir otro usuario |
| Ya te postulaste a este trabajo | Solo se permite una postulación por trabajo |
| El trabajo está cerrado y no acepta nuevas postulaciones | El empleador cerró la publicación |
| El teléfono solo está disponible cuando la postulación fue aceptada | Todavía no fue aceptada |
| La sesión de seguridad expiró. Recargá la página… | Token de seguridad vencido: recargar la página |
