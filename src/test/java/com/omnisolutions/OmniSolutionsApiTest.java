package com.omnisolutions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisolutions.repository.UsuarioRepository;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Pruebas de integración de punta a punta sobre la API (Spring Security real, sesión, CSRF,
 * servicios y JPA sobre H2 en modo MySQL).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OmniSolutionsApiTest {

    private static final AtomicInteger SECUENCIA = new AtomicInteger();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // =================================================================== usuarios

    @Test
    void registroExitosoGuardaContrasenaConBCrypt() throws Exception {
        String usuario = nuevoNombre("reg");
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Juan Pérez", usuario, "EMPLEADO", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value(usuario))
                .andExpect(jsonPath("$.tipoUsuario").value("EMPLEADO"))
                .andExpect(jsonPath("$.fechaRegistro").exists())
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        String hash = usuarioRepository.findByUsuario(usuario).orElseThrow().getContrasena();
        assertThat(hash).startsWith("$2a$").isNotEqualTo("clave123");
    }

    @Test
    void registroConUsuarioDuplicadoDevuelve409() throws Exception {
        String usuario = nuevoNombre("dup");
        registrar(usuario, "EMPLEADOR", null);
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Otro", usuario.toUpperCase(), "EMPLEADO", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("ya está en uso")));
    }

    @Test
    void registroConDatosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"usuario\":\"a b\",\"contrasena\":\"123\",\"tipoUsuario\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.usuario").exists())
                .andExpect(jsonPath("$.errores.contrasena").exists())
                .andExpect(jsonPath("$.errores.tipoUsuario").exists());
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Rol Falso", nuevoNombre("rol"), "ADMIN", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inicioDeSesionCorrectoEIncorrecto() throws Exception {
        String usuario = nuevoNombre("login");
        registrar(usuario, "EMPLEADO", null);

        mvc.perform(post("/api/auth/login").with(csrf()).param("usuario", usuario).param("contrasena", "clave123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value(usuario));
        mvc.perform(post("/api/auth/login").with(csrf()).param("usuario", usuario).param("contrasena", "incorrecta"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
        mvc.perform(post("/api/auth/login").with(csrf()).param("usuario", "no-existe").param("contrasena", "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cierreDeSesionInvalidaLaSesion() throws Exception {
        MockHttpSession s = registrarYLoguear("EMPLEADO", null);
        mvc.perform(get("/api/usuarios/me").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(s).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/me").session(s)).andExpect(status().isUnauthorized());
    }

    @Test
    void modificarPerfilNoPermiteCambiarElRol() throws Exception {
        MockHttpSession s = registrarYLoguear("EMPLEADO", null);
        mvc.perform(put("/api/usuarios/me").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nombre Nuevo\",\"telefono\":\"+54 351 4000000\",\"tipoUsuario\":\"EMPLEADOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre Nuevo"))
                .andExpect(jsonPath("$.telefono").value("+54 351 4000000"))
                .andExpect(jsonPath("$.tipoUsuario").value("EMPLEADO"));

        mvc.perform(put("/api/usuarios/me").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nombre Nuevo\",\"telefono\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").doesNotExist());

        mvc.perform(put("/api/usuarios/me").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nombre Nuevo\",\"telefono\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.telefono").exists());
    }

    // =================================================================== publicaciones

    @Test
    void publicacionSegunRol() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession empleado = registrarYLoguear("EMPLEADO", null);

        crearPublicacion(empleador, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("TRABAJO"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.fechaPublicacion").exists());
        crearPublicacion(empleado, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SERVICIO"));

        crearPublicacion(empleador, "SERVICIO").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Los empleadores solo pueden publicar trabajos"));
        crearPublicacion(empleado, "TRABAJO").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Los empleados solo pueden publicar servicios"));
    }

    @Test
    void publicacionConDatosInvalidosDevuelve400() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        mvc.perform(post("/api/publicaciones").session(empleador).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"abc\",\"descripcion\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.titulo").exists())
                .andExpect(jsonPath("$.errores.descripcion").exists());
    }

    @Test
    void modificarCerrarYEliminarPublicacionPropia() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        long id = idDe(crearPublicacion(empleador, null));

        mvc.perform(put("/api/publicaciones/" + id).session(empleador).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publicacionJson(null, "Título modificado", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Título modificado"))
                .andExpect(jsonPath("$.ubicacion").doesNotExist());

        mvc.perform(get("/api/publicaciones/mias").session(empleador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].propia").value(true));

        mvc.perform(post("/api/publicaciones/" + id + "/cerrar").session(empleador).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        mvc.perform(delete("/api/publicaciones/" + id).session(empleador).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/publicaciones/" + id).session(empleador))
                .andExpect(status().isNotFound());
    }

    @Test
    void noSePuedenAdministrarPublicacionesAjenas() throws Exception {
        MockHttpSession duenio = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession otro = registrarYLoguear("EMPLEADOR", null);
        long id = idDe(crearPublicacion(duenio, null));

        mvc.perform(put("/api/publicaciones/" + id).session(otro).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publicacionJson(null, "Intento de cambio", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No podés modificar publicaciones de otros usuarios"));
        mvc.perform(post("/api/publicaciones/" + id + "/cerrar").session(otro).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/publicaciones/" + id).session(otro).with(csrf()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/publicaciones/" + id).session(duenio))
                .andExpect(jsonPath("$.titulo").value("Se busca ayudante de cocina"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void publicacionesCerradasNoAparecenEnElFeed() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        String titulo = "Feed " + nuevoNombre("t");
        long id = idDe(mvc.perform(post("/api/publicaciones").session(empleador).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(publicacionJson(null, titulo, "Rosario"))));

        mvc.perform(get("/api/publicaciones").param("q", titulo).session(empleador))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/publicaciones").param("tipo", "SERVICIO").param("q", titulo).session(empleador))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/publicaciones/" + id + "/cerrar").session(empleador).with(csrf()));
        mvc.perform(get("/api/publicaciones").param("q", titulo).session(empleador))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =================================================================== postulaciones

    @Test
    void postulacionValidaYDuplicada() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession empleado = registrarYLoguear("EMPLEADO", null);
        long trabajo = idDe(crearPublicacion(empleador, null));

        postular(empleado, trabajo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.publicacionId").value(trabajo));
        postular(empleado, trabajo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya te postulaste a este trabajo"));

        mvc.perform(get("/api/postulaciones/realizadas").session(empleado))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/postulaciones/recibidas").session(empleador))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/publicaciones/" + trabajo).session(empleado))
                .andExpect(jsonPath("$.miPostulacion").value("PENDIENTE"))
                .andExpect(jsonPath("$.puedePostularse").value(false));
        mvc.perform(get("/api/publicaciones/" + trabajo).session(empleador))
                .andExpect(jsonPath("$.cantidadPostulaciones").value(1));
    }

    @Test
    void postulacionAServicioTrabajoCerradoOPorEmpleadorSeRechaza() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession empleado = registrarYLoguear("EMPLEADO", null);
        MockHttpSession otroEmpleado = registrarYLoguear("EMPLEADO", null);
        long servicio = idDe(crearPublicacion(otroEmpleado, null));
        long trabajo = idDe(crearPublicacion(empleador, null));

        postular(empleado, servicio).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Solo es posible postularse a publicaciones de tipo trabajo"));

        postular(empleador, trabajo).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo los empleados pueden postularse a trabajos"));

        mvc.perform(post("/api/publicaciones/" + trabajo + "/cerrar").session(empleador).with(csrf()))
                .andExpect(status().isOk());
        postular(empleado, trabajo).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El trabajo está cerrado y no acepta nuevas postulaciones"));

        postular(empleado, 999_999).andExpect(status().isNotFound());
    }

    @Test
    void variasPostulacionesAceptadasYRechazoIndependientes() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession e1 = registrarYLoguear("EMPLEADO", null);
        MockHttpSession e2 = registrarYLoguear("EMPLEADO", null);
        MockHttpSession e3 = registrarYLoguear("EMPLEADO", null);
        long trabajo = idDe(crearPublicacion(empleador, null));
        long p1 = idDe(postular(e1, trabajo));
        long p2 = idDe(postular(e2, trabajo));
        long p3 = idDe(postular(e3, trabajo));

        resolver(empleador, p1, "aceptar").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));
        // Aceptar una no modifica las demás.
        mvc.perform(get("/api/postulaciones/realizadas").session(e2))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));

        resolver(empleador, p2, "aceptar").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));
        resolver(empleador, p3, "rechazar").andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));

        mvc.perform(get("/api/postulaciones/realizadas").session(e1))
                .andExpect(jsonPath("$[0].estado").value("ACEPTADA"));
        mvc.perform(get("/api/postulaciones/recibidas").param("publicacionId", String.valueOf(trabajo))
                        .session(empleador))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.estado == 'ACEPTADA')]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.estado == 'RECHAZADA')]", hasSize(1)));

        // No se puede modificar una postulación ya resuelta.
        resolver(empleador, p1, "rechazar").andExpect(status().isConflict());
        resolver(empleador, p3, "aceptar").andExpect(status().isConflict());
    }

    @Test
    void soloElEmpleadorPropietarioGestionaPostulaciones() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession otroEmpleador = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession empleado = registrarYLoguear("EMPLEADO", null);
        long trabajo = idDe(crearPublicacion(empleador, null));
        long postulacion = idDe(postular(empleado, trabajo));

        resolver(otroEmpleador, postulacion, "aceptar").andExpect(status().isForbidden());
        resolver(empleado, postulacion, "aceptar").andExpect(status().isForbidden());
        mvc.perform(get("/api/postulaciones/recibidas").param("publicacionId", String.valueOf(trabajo))
                        .session(otroEmpleador))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/postulaciones/recibidas").session(otroEmpleador))
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =================================================================== contacto

    @Test
    void telefonoSoloDisponibleTrasLaAceptacion() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", "351-1111111");
        MockHttpSession empleado = registrarYLoguear("EMPLEADO", "351-2222222");
        MockHttpSession tercero = registrarYLoguear("EMPLEADO", "351-3333333");
        long trabajo = idDe(crearPublicacion(empleador, null));
        long postulacion = idDe(postular(empleado, trabajo));

        // Antes de aceptar: ninguno ve el teléfono del otro.
        contacto(empleado, postulacion).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(containsString("aceptada")));
        contacto(empleador, postulacion).andExpect(status().isForbidden());

        resolver(empleador, postulacion, "aceptar").andExpect(status().isOk());

        // Después de aceptar: cada parte ve el teléfono de la otra.
        contacto(empleado, postulacion).andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("351-1111111"))
                .andExpect(jsonPath("$.telefonoDisponible").value(true));
        contacto(empleador, postulacion).andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("351-2222222"));
        // Un tercero nunca accede.
        contacto(tercero, postulacion).andExpect(status().isForbidden());
    }

    @Test
    void telefonoNoProporcionadoMuestraMensaje() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        MockHttpSession empleado = registrarYLoguear("EMPLEADO", "351-4444444");
        long trabajo = idDe(crearPublicacion(empleador, null));
        long postulacion = idDe(postular(empleado, trabajo));
        resolver(empleador, postulacion, "aceptar");

        contacto(empleado, postulacion).andExpect(status().isOk())
                .andExpect(jsonPath("$.telefonoDisponible").value(false))
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.mensaje").value(containsString("no proporcionó un teléfono")));
    }

    @Test
    void telefonosNoSeExponenEnListadosNiDetalles() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", "999-0000001");
        MockHttpSession empleado = registrarYLoguear("EMPLEADO", "999-0000002");
        long trabajo = idDe(crearPublicacion(empleador, null));
        long postulacion = idDe(postular(empleado, trabajo));
        resolver(empleador, postulacion, "aceptar");

        for (MockHttpSession s : new MockHttpSession[]{empleador, empleado}) {
            for (String url : new String[]{"/api/publicaciones", "/api/publicaciones/" + trabajo,
                    "/api/postulaciones/realizadas", "/api/postulaciones/recibidas", "/api/publicaciones/mias"}) {
                mvc.perform(get(url).session(s))
                        .andExpect(status().isOk())
                        .andExpect(content().string(not(containsString("999-000000"))))
                        .andExpect(content().string(not(containsString("telefono"))));
            }
        }
    }

    // =================================================================== protección

    @Test
    void endpointsProtegidosSinSesion() throws Exception {
        for (String url : new String[]{"/api/publicaciones", "/api/publicaciones/1", "/api/publicaciones/mias",
                "/api/usuarios/me", "/api/postulaciones/realizadas", "/api/postulaciones/recibidas",
                "/api/postulaciones/1/contacto"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/publicaciones").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(publicacionJson(null, "Sin sesión", null)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/postulaciones/1/aceptar").with(csrf())).andExpect(status().isUnauthorized());

        // Las páginas privadas redirigen al inicio de sesión; la pública es accesible.
        mvc.perform(get("/inicio.html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    void operacionesSinTokenCsrfSonRechazadas() throws Exception {
        MockHttpSession empleador = registrarYLoguear("EMPLEADOR", null);
        mvc.perform(post("/api/publicaciones").session(empleador).contentType(MediaType.APPLICATION_JSON)
                        .content(publicacionJson(null, "Sin token CSRF", null)))
                .andExpect(status().isForbidden());
    }

    // =================================================================== utilidades

    private static String nuevoNombre(String prefijo) {
        return prefijo + "_" + System.nanoTime() % 100000 + "_" + SECUENCIA.incrementAndGet();
    }

    private String registroJson(String nombre, String usuario, String rol, String telefono) throws Exception {
        var datos = new java.util.HashMap<String, Object>();
        datos.put("nombre", nombre);
        datos.put("usuario", usuario);
        datos.put("contrasena", "clave123");
        datos.put("tipoUsuario", rol);
        datos.put("telefono", telefono);
        return json.writeValueAsString(datos);
    }

    private String publicacionJson(String tipo, String titulo, String ubicacion) throws Exception {
        var datos = new java.util.HashMap<String, Object>();
        datos.put("tipo", tipo);
        datos.put("titulo", titulo);
        datos.put("descripcion", "Descripción de prueba con suficiente detalle.");
        datos.put("ubicacion", ubicacion);
        return json.writeValueAsString(datos);
    }

    private void registrar(String usuario, String rol, String telefono) throws Exception {
        mvc.perform(post("/api/auth/registro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Usuario " + usuario, usuario, rol, telefono)))
                .andExpect(status().isCreated());
    }

    private MockHttpSession registrarYLoguear(String rol, String telefono) throws Exception {
        String usuario = nuevoNombre(rol.toLowerCase());
        registrar(usuario, rol, telefono);
        MvcResult r = mvc.perform(post("/api/auth/login").with(csrf())
                        .param("usuario", usuario).param("contrasena", "clave123"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    private ResultActions crearPublicacion(MockHttpSession s, String tipo) throws Exception {
        return mvc.perform(post("/api/publicaciones").session(s).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicacionJson(tipo, "Se busca ayudante de cocina", "Córdoba")));
    }

    private ResultActions postular(MockHttpSession s, long idPublicacion) throws Exception {
        return mvc.perform(post("/api/publicaciones/" + idPublicacion + "/postulaciones").session(s).with(csrf()));
    }

    private ResultActions resolver(MockHttpSession s, long idPostulacion, String accion) throws Exception {
        MockHttpServletRequestBuilder req = post("/api/postulaciones/" + idPostulacion + "/" + accion);
        return mvc.perform(req.session(s).with(csrf()));
    }

    private ResultActions contacto(MockHttpSession s, long idPostulacion) throws Exception {
        return mvc.perform(get("/api/postulaciones/" + idPostulacion + "/contacto").session(s));
    }

    private long idDe(ResultActions acciones) throws Exception {
        JsonNode nodo = json.readTree(acciones.andReturn().getResponse().getContentAsString());
        return nodo.get("id").asLong();
    }
}
