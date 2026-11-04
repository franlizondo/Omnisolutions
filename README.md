# Omni Solutions — Beta

Plataforma web de intermediación entre **empleadores** y **empleados**. Los empleadores publican trabajos,
los empleados ofrecen servicios y se postulan a trabajos, y el teléfono de contacto se comparte
**solo cuando el empleador acepta una postulación**.

Versión beta funcional: frontend, backend y base de datos MySQL integrados.

## Funcionalidades

| Área | Implementado |
|---|---|
| Usuarios | Registro, inicio y cierre de sesión, modificación de perfil (nombre y teléfono). Rol fijo (EMPLEADOR / EMPLEADO). Contraseñas con BCrypt. |
| Publicaciones | Crear, consultar (feed con filtro por tipo y búsqueda por texto), ver detalle, modificar, cerrar, eliminar y listar las propias. El empleador publica TRABAJOS y el empleado, SERVICIOS. |
| Postulaciones | Postularse (solo empleados, solo a trabajos activos, sin duplicados), consultar realizadas y recibidas, aceptar y rechazar (cada una por separado; varias aceptadas por trabajo). |
| Contacto | Teléfono visible únicamente entre las partes de una postulación ACEPTADA. Mensaje si no fue proporcionado. Nunca aparece en listados ni búsquedas. |
| Seguridad | Spring Security con sesión, CSRF, control de rol y de propiedad en el backend, validaciones, respuestas de error JSON uniformes, CSP. |

## Tecnologías

Java 21 · Spring Boot 3.5 (Web, Data JPA, Security, Validation) · Maven · MySQL 8 · HTML, CSS y JavaScript sin frameworks (servidos desde `src/main/resources/static`).

## Estructura

```
omni-solutions/
├── pom.xml
├── .env.example                      # variables de conexión (copiar como .env)
├── database/
│   ├── 01-crear-base-datos.sql       # base, usuario MySQL y tablas
│   └── 02-datos-prueba.sql           # usuarios, publicaciones y postulaciones de ejemplo
├── docs/
│   ├── diseno-bd.md                  # modelo de datos
│   ├── manual-usuario.md             # uso de cada pantalla
│   └── proyecto.md                   # alcance, arquitectura, reglas, pruebas y pendientes
└── src/
    ├── main/java/com/omnisolutions/
    │   ├── OmniSolutionsApplication.java
    │   ├── config/        SecurityConfig.java
    │   ├── controller/    Auth, Usuario, Publicacion, Postulacion, GlobalExceptionHandler
    │   ├── dto/           UsuarioDtos, PublicacionDtos, PostulacionDtos
    │   ├── model/         Usuario, Publicacion, Postulacion + enums
    │   ├── repository/    repositorios Spring Data JPA
    │   └── service/       UsuarioService, PublicacionService, PostulacionService, ApiException
    ├── main/resources/
    │   ├── application.properties
    │   ├── schema.sql                # se ejecuta al iniciar (CREATE TABLE IF NOT EXISTS)
    │   └── static/                   # index, inicio, publicar, detalle, mis-publicaciones,
    │                                 # postulaciones, perfil + css/ + js/
    └── test/
        ├── java/.../OmniSolutionsApiTest.java   # 20 pruebas de integración
        └── e2e/e2e-navegador.js                 # prueba de navegador (opcional, Node + Playwright)
```

## Requisitos

- JDK 21
- Maven 3.9+ (o el Maven incluido en NetBeans)
- MySQL 8.0+

## 1. Configurar MySQL

1. Editar `database/01-crear-base-datos.sql` y reemplazar `cambiar_esta_contrasena` por una contraseña propia.
2. Ejecutarlo con un usuario administrador:
   ```bash
   mysql -u root -p < database/01-crear-base-datos.sql
   ```
   Crea la base `omni_solutions` (utf8mb4), el usuario `omni_app` con permisos solo sobre esa base y las tres tablas.
3. (Opcional) Cargar datos de prueba:
   ```bash
   mysql -u omni_app -p omni_solutions < database/02-datos-prueba.sql
   ```

> Si las tablas no existen, la aplicación las crea al iniciar (`schema.sql`); solo es imprescindible que existan la base y el usuario MySQL.

## 2. Configurar la conexión

Copiar `.env.example` como `.env` en la raíz del proyecto y completar los valores:

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=omni_solutions
DB_USER=omni_app
DB_PASSWORD=la_contrasena_elegida
DB_INIT_MODE=always
SERVER_PORT=8080
```

La aplicación lee primero las **variables de entorno** del sistema y, si no existen, el archivo `.env`
(debe estar en el directorio desde donde se ejecuta la aplicación, normalmente la raíz del proyecto).
El archivo `.env` está en `.gitignore`: nunca se sube al repositorio.

## 3. Ejecutar

### Con Maven

```bash
mvn spring-boot:run
```

o bien generar el JAR y ejecutarlo:

```bash
mvn clean package
java -jar target/omni-solutions-0.1.0-beta.jar
```

Abrir **http://localhost:8080**.

### Con NetBeans

1. *File → Open Project* y seleccionar la carpeta del proyecto (NetBeans lo reconoce como proyecto Maven).
2. Crear el archivo `.env` en la raíz del proyecto (paso 2).
3. Ejecutar con **Run Project (F6)**. La primera vez, si NetBeans pide la clase principal, elegir
   `com.omnisolutions.OmniSolutionsApplication`.
   Alternativa: clic derecho sobre el proyecto → *Run Maven → Goals…* → `spring-boot:run`.
4. Abrir http://localhost:8080.

Si se prefiere no usar `.env`, se pueden definir las variables en *Project Properties → Actions → Run Project → Set Properties*
(por ejemplo `Env.DB_USER=omni_app`).

### Usuarios de prueba

Disponibles después de ejecutar `02-datos-prueba.sql`. Contraseña de todos: **`Omni2026`**

| Usuario | Rol | Teléfono |
|---|---|---|
| `panaderia.sol` | Empleador | 351 400-1000 |
| `constructora.norte` | Empleador | (no proporcionado) |
| `lucia.fernandez` | Empleado | 351 600-2000 |
| `diego.morales` | Empleado | 351 600-3000 |
| `camila.ortiz` | Empleado | (no proporcionado) |

## 4. Pruebas

```bash
mvn test
```

Ejecuta 20 pruebas de integración (Spring Security real, sesión, CSRF, servicios y JPA) sobre una base H2 en memoria
en modo MySQL, por lo que no requieren un servidor MySQL. Para ejecutarlas contra MySQL real, usar una base vacía
**distinta** de la de trabajo:

```bash
mvn test -Dspring.datasource.url="jdbc:mysql://localhost:3306/omni_solutions_test?useSSL=false&allowPublicKeyRetrieval=true" \
         -Dspring.datasource.username=omni_app -Dspring.datasource.password=... \
         -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
         -Dspring.sql.init.mode=always -Dspring.jpa.hibernate.ddl-auto=validate
```

El detalle de los casos y resultados está en [docs/proyecto.md](docs/proyecto.md#7-pruebas).

## API REST

Todas las rutas bajo `/api` (excepto `/api/auth/**`) requieren sesión. Las operaciones que modifican datos
requieren el encabezado `X-XSRF-TOKEN` con el valor de la cookie `XSRF-TOKEN`.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/auth/csrf` | Obtiene el token CSRF |
| POST | `/api/auth/registro` | Registro (JSON) |
| POST | `/api/auth/login` | Inicio de sesión (form: `usuario`, `contrasena`) |
| POST | `/api/auth/logout` | Cierre de sesión |
| GET / PUT | `/api/usuarios/me` | Perfil propio / modificar nombre y teléfono |
| GET | `/api/publicaciones?tipo=&q=` | Feed de publicaciones activas |
| GET | `/api/publicaciones/mias` | Publicaciones propias |
| GET | `/api/publicaciones/{id}` | Detalle |
| POST | `/api/publicaciones` | Crear |
| PUT | `/api/publicaciones/{id}` | Modificar (solo propietario) |
| POST | `/api/publicaciones/{id}/cerrar` | Cerrar (solo propietario) |
| DELETE | `/api/publicaciones/{id}` | Eliminar (solo propietario) |
| POST | `/api/publicaciones/{id}/postulaciones` | Postularse (solo empleados) |
| GET | `/api/postulaciones/realizadas` | Postulaciones del empleado |
| GET | `/api/postulaciones/recibidas?publicacionId=` | Postulaciones a los trabajos del empleador |
| POST | `/api/postulaciones/{id}/aceptar` · `/rechazar` | Resolver (solo empleador propietario) |
| GET | `/api/postulaciones/{id}/contacto` | Teléfono de la contraparte (solo si está ACEPTADA) |

Errores: `{"status": 403, "mensaje": "...", "errores": {"campo": "..."}}` con códigos 400, 401, 403, 404 y 409.

## Documentación

- [Diseño de base de datos](docs/diseno-bd.md)
- [Manual de usuario](docs/manual-usuario.md)
- [Documento del proyecto](docs/proyecto.md) — arquitectura, reglas de negocio, decisiones, pruebas y pendientes.
