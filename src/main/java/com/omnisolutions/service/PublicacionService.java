package com.omnisolutions.service;

import com.omnisolutions.dto.PublicacionDtos.PublicacionRequest;
import com.omnisolutions.dto.PublicacionDtos.PublicacionResponse;
import com.omnisolutions.model.EstadoPostulacion;
import com.omnisolutions.model.EstadoPublicacion;
import com.omnisolutions.model.Postulacion;
import com.omnisolutions.model.Publicacion;
import com.omnisolutions.model.TipoPublicacion;
import com.omnisolutions.model.TipoUsuario;
import com.omnisolutions.model.Usuario;
import com.omnisolutions.repository.PostulacionRepository;
import com.omnisolutions.repository.PublicacionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicacionService {

    private final PublicacionRepository publicacionRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioService usuarioService;

    public PublicacionService(PublicacionRepository publicacionRepository,
                              PostulacionRepository postulacionRepository,
                              UsuarioService usuarioService) {
        this.publicacionRepository = publicacionRepository;
        this.postulacionRepository = postulacionRepository;
        this.usuarioService = usuarioService;
    }

    /** Tipo de publicación que corresponde a cada rol. */
    public static TipoPublicacion tipoPermitido(TipoUsuario rol) {
        return rol == TipoUsuario.EMPLEADOR ? TipoPublicacion.TRABAJO : TipoPublicacion.SERVICIO;
    }

    // ------------------------------------------------------------------ consultas

    /** Publicaciones ACTIVAS (las cerradas nunca aparecen en el feed). */
    @Transactional(readOnly = true)
    public List<PublicacionResponse> feed(TipoPublicacion tipo, String texto) {
        Usuario actual = usuarioService.actual();
        String patron = (texto == null || texto.isBlank())
                ? null : "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
        List<Publicacion> lista = publicacionRepository.buscarFeed(EstadoPublicacion.ACTIVA, tipo, patron);
        return mapearLista(actual, lista);
    }

    @Transactional(readOnly = true)
    public List<PublicacionResponse> mias() {
        Usuario actual = usuarioService.actual();
        return mapearLista(actual, publicacionRepository.buscarPorUsuario(actual));
    }

    /** Detalle visible para cualquier usuario autenticado (incluso si está cerrada). */
    @Transactional(readOnly = true)
    public PublicacionResponse detalle(Long id) {
        Usuario actual = usuarioService.actual();
        Publicacion p = buscar(id);
        Long cantidad = p.esDe(actual) && p.getTipo() == TipoPublicacion.TRABAJO
                ? postulacionRepository.countByPublicacion(p) : null;
        Postulacion mia = actual.getTipoUsuario() == TipoUsuario.EMPLEADO
                ? postulacionRepository.findByUsuarioAndPublicacion(actual, p).orElse(null) : null;
        return mapear(actual, p, cantidad, mia == null ? null : mia.getEstado(), mia == null ? null : mia.getId());
    }

    // ------------------------------------------------------------------ administración

    @Transactional
    public PublicacionResponse crear(PublicacionRequest req) {
        Usuario actual = usuarioService.actual();
        TipoPublicacion permitido = tipoPermitido(actual.getTipoUsuario());
        if (req.tipo() != null && req.tipo() != permitido) {
            throw ApiException.prohibido(actual.getTipoUsuario() == TipoUsuario.EMPLEADOR
                    ? "Los empleadores solo pueden publicar trabajos"
                    : "Los empleados solo pueden publicar servicios");
        }
        Publicacion p = new Publicacion();
        p.setUsuario(actual);
        p.setTipo(permitido);
        p.setEstado(EstadoPublicacion.ACTIVA);
        aplicarDatos(p, req);
        p = publicacionRepository.save(p);
        return mapear(actual, p, p.getTipo() == TipoPublicacion.TRABAJO ? 0L : null, null, null);
    }

    @Transactional
    public PublicacionResponse modificar(Long id, PublicacionRequest req) {
        Publicacion p = buscarPropia(id, "modificar");
        if (req.tipo() != null && req.tipo() != p.getTipo()) {
            throw ApiException.invalido("El tipo de una publicación no se puede cambiar");
        }
        aplicarDatos(p, req);
        return detalleDe(p);
    }

    @Transactional
    public PublicacionResponse cerrar(Long id) {
        Publicacion p = buscarPropia(id, "cerrar");
        p.setEstado(EstadoPublicacion.CERRADA);
        return detalleDe(p);
    }

    @Transactional
    public void eliminar(Long id) {
        Publicacion p = buscarPropia(id, "eliminar");
        postulacionRepository.eliminarPorPublicacion(p);
        publicacionRepository.delete(p);
    }

    // ------------------------------------------------------------------ auxiliares

    public Publicacion buscar(Long id) {
        return publicacionRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("La publicación no existe"));
    }

    private Publicacion buscarPropia(Long id, String accion) {
        Publicacion p = buscar(id);
        if (!p.esDe(usuarioService.actual())) {
            throw ApiException.prohibido("No podés " + accion + " publicaciones de otros usuarios");
        }
        return p;
    }

    private PublicacionResponse detalleDe(Publicacion p) {
        return mapear(p.getUsuario(), p,
                p.getTipo() == TipoPublicacion.TRABAJO ? postulacionRepository.countByPublicacion(p) : null,
                null, null);
    }

    private static void aplicarDatos(Publicacion p, PublicacionRequest req) {
        p.setTitulo(req.titulo().trim());
        p.setDescripcion(req.descripcion().trim());
        p.setUbicacion(UsuarioService.textoOpcional(req.ubicacion()));
    }

    /** Mapea una lista resolviendo en una sola consulta los datos de contexto del usuario actual. */
    private List<PublicacionResponse> mapearLista(Usuario actual, List<Publicacion> lista) {
        Map<Long, EstadoPostulacion> misEstados = new HashMap<>();
        Map<Long, Long> cantidades = new HashMap<>();
        if (actual.getTipoUsuario() == TipoUsuario.EMPLEADO) {
            for (Object[] fila : postulacionRepository.buscarEstadosPorUsuario(actual)) {
                misEstados.put((Long) fila[0], (EstadoPostulacion) fila[1]);
            }
        } else {
            for (Object[] fila : postulacionRepository.contarPorPublicacionDe(actual)) {
                cantidades.put((Long) fila[0], (Long) fila[1]);
            }
        }
        return lista.stream().map(p -> mapear(actual, p,
                p.esDe(actual) && p.getTipo() == TipoPublicacion.TRABAJO ? cantidades.getOrDefault(p.getId(), 0L) : null,
                misEstados.get(p.getId()), null)).toList();
    }

    private static PublicacionResponse mapear(Usuario actual, Publicacion p, Long cantidad,
                                              EstadoPostulacion miEstado, Long miPostulacionId) {
        boolean puedePostularse = actual.getTipoUsuario() == TipoUsuario.EMPLEADO
                && p.getTipo() == TipoPublicacion.TRABAJO
                && p.estaActiva()
                && miEstado == null;
        Usuario autor = p.getUsuario();
        return new PublicacionResponse(p.getId(), p.getTipo(), p.getTitulo(), p.getDescripcion(),
                p.getUbicacion(), p.getFechaPublicacion(), p.getEstado(),
                autor.getId(), autor.getNombre(), autor.getTipoUsuario(),
                p.esDe(actual), cantidad, miEstado, miPostulacionId, puedePostularse);
    }
}
