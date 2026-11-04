package com.omnisolutions.service;

import com.omnisolutions.dto.PostulacionDtos.ContactoResponse;
import com.omnisolutions.dto.PostulacionDtos.PostulacionResponse;
import com.omnisolutions.model.EstadoPostulacion;
import com.omnisolutions.model.Postulacion;
import com.omnisolutions.model.Publicacion;
import com.omnisolutions.model.TipoPublicacion;
import com.omnisolutions.model.TipoUsuario;
import com.omnisolutions.model.Usuario;
import com.omnisolutions.repository.PostulacionRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostulacionService {

    private final PostulacionRepository postulacionRepository;
    private final PublicacionService publicacionService;
    private final UsuarioService usuarioService;

    public PostulacionService(PostulacionRepository postulacionRepository,
                              PublicacionService publicacionService,
                              UsuarioService usuarioService) {
        this.postulacionRepository = postulacionRepository;
        this.publicacionService = publicacionService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public PostulacionResponse postular(Long idPublicacion) {
        Usuario actual = usuarioService.actual();
        if (actual.getTipoUsuario() != TipoUsuario.EMPLEADO) {
            throw ApiException.prohibido("Solo los empleados pueden postularse a trabajos");
        }
        Publicacion publicacion = publicacionService.buscar(idPublicacion);
        if (publicacion.getTipo() != TipoPublicacion.TRABAJO) {
            throw ApiException.invalido("Solo es posible postularse a publicaciones de tipo trabajo");
        }
        if (!publicacion.estaActiva()) {
            throw ApiException.conflicto("El trabajo está cerrado y no acepta nuevas postulaciones");
        }
        if (postulacionRepository.existsByUsuarioAndPublicacion(actual, publicacion)) {
            throw ApiException.conflicto("Ya te postulaste a este trabajo");
        }
        Postulacion p = new Postulacion();
        p.setUsuario(actual);
        p.setPublicacion(publicacion);
        p.setEstado(EstadoPostulacion.PENDIENTE);
        try {
            return PostulacionResponse.de(postulacionRepository.saveAndFlush(p));
        } catch (DataIntegrityViolationException e) {
            // Dos solicitudes simultáneas: la restricción UNIQUE de la base lo impide.
            throw ApiException.conflicto("Ya te postulaste a este trabajo");
        }
    }

    @Transactional(readOnly = true)
    public List<PostulacionResponse> realizadas() {
        return postulacionRepository.buscarRealizadas(usuarioService.actual()).stream()
                .map(PostulacionResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PostulacionResponse> recibidas(Long idPublicacion) {
        Usuario actual = usuarioService.actual();
        if (idPublicacion != null && !publicacionService.buscar(idPublicacion).esDe(actual)) {
            throw ApiException.prohibido("Solo el propietario puede ver las postulaciones de esta publicación");
        }
        return postulacionRepository.buscarRecibidas(actual, idPublicacion).stream()
                .map(PostulacionResponse::de).toList();
    }

    /**
     * Acepta o rechaza una postulación PENDIENTE. Cada postulación se resuelve de forma
     * independiente: no modifica el estado de las demás postulaciones del mismo trabajo.
     */
    @Transactional
    public PostulacionResponse resolver(Long id, EstadoPostulacion nuevoEstado) {
        Postulacion p = buscar(id);
        if (!p.getPublicacion().esDe(usuarioService.actual())) {
            throw ApiException.prohibido("Solo el empleador propietario del trabajo puede gestionar esta postulación");
        }
        if (p.getEstado() != EstadoPostulacion.PENDIENTE) {
            throw ApiException.conflicto("La postulación ya fue " + p.getEstado().name().toLowerCase()
                    + " y no puede modificarse");
        }
        p.setEstado(nuevoEstado);
        return PostulacionResponse.de(p);
    }

    /**
     * Devuelve el teléfono de la contraparte solo si la postulación fue ACEPTADA:
     * el empleado ve el del empleador y el empleador ve el del empleado.
     */
    @Transactional(readOnly = true)
    public ContactoResponse contacto(Long id) {
        Usuario actual = usuarioService.actual();
        Postulacion p = buscar(id);
        Usuario empleado = p.getUsuario();
        Usuario empleador = p.getPublicacion().getUsuario();
        Usuario contraparte;
        if (empleado.getId().equals(actual.getId())) {
            contraparte = empleador;
        } else if (empleador.getId().equals(actual.getId())) {
            contraparte = empleado;
        } else {
            throw ApiException.prohibido("No participás en esta postulación");
        }
        if (p.getEstado() != EstadoPostulacion.ACEPTADA) {
            throw ApiException.prohibido("El teléfono solo está disponible cuando la postulación fue aceptada");
        }
        String telefono = contraparte.getTelefono();
        boolean disponible = telefono != null && !telefono.isBlank();
        return new ContactoResponse(p.getId(), contraparte.getNombre(), disponible ? telefono : null, disponible,
                disponible ? "Teléfono de contacto disponible" : contraparte.getNombre() + " no proporcionó un teléfono");
    }

    private Postulacion buscar(Long id) {
        return postulacionRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("La postulación no existe"));
    }
}
