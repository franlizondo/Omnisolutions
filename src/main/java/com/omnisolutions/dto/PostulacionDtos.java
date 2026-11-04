package com.omnisolutions.dto;

import com.omnisolutions.model.EstadoPostulacion;
import com.omnisolutions.model.EstadoPublicacion;
import com.omnisolutions.model.Postulacion;
import java.time.LocalDateTime;

/** Objetos de transferencia relacionados con postulaciones. Nunca incluyen teléfonos. */
public final class PostulacionDtos {

    private PostulacionDtos() {
    }

    public record PostulacionResponse(
            Long id,
            LocalDateTime fechaPostulacion,
            EstadoPostulacion estado,
            Long publicacionId,
            String publicacionTitulo,
            EstadoPublicacion publicacionEstado,
            String postulanteNombre,
            String empleadorNombre) {

        public static PostulacionResponse de(Postulacion p) {
            return new PostulacionResponse(p.getId(), p.getFechaPostulacion(), p.getEstado(),
                    p.getPublicacion().getId(), p.getPublicacion().getTitulo(), p.getPublicacion().getEstado(),
                    p.getUsuario().getNombre(), p.getPublicacion().getUsuario().getNombre());
        }
    }

    /** Datos de contacto de la contraparte; solo se entregan con una postulación ACEPTADA. */
    public record ContactoResponse(Long postulacionId, String nombre, String telefono,
                                   boolean telefonoDisponible, String mensaje) {
    }
}
