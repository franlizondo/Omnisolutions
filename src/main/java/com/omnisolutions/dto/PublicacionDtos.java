package com.omnisolutions.dto;

import com.omnisolutions.model.EstadoPostulacion;
import com.omnisolutions.model.EstadoPublicacion;
import com.omnisolutions.model.TipoPublicacion;
import com.omnisolutions.model.TipoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Objetos de transferencia relacionados con publicaciones. */
public final class PublicacionDtos {

    private PublicacionDtos() {
    }

    /**
     * Alta / modificación. El tipo es opcional: si no se envía se deduce del rol
     * (EMPLEADOR = TRABAJO, EMPLEADO = SERVICIO). Si se envía y no coincide con el rol, se rechaza.
     */
    public record PublicacionRequest(
            TipoPublicacion tipo,

            @NotBlank(message = "El título es obligatorio")
            @Size(min = 5, max = 120, message = "El título debe tener entre 5 y 120 caracteres")
            String titulo,

            @NotBlank(message = "La descripción es obligatoria")
            @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
            String descripcion,

            @Size(max = 120, message = "La ubicación admite como máximo 120 caracteres")
            String ubicacion) {
    }

    /**
     * Vista pública de una publicación. No contiene datos privados del autor (ni teléfono ni usuario).
     * Los campos de contexto dependen de quién consulta:
     * - cantidadPostulaciones: solo para el propietario de un trabajo.
     * - miPostulacion / miPostulacionId: solo para el empleado que se postuló.
     */
    public record PublicacionResponse(
            Long id,
            TipoPublicacion tipo,
            String titulo,
            String descripcion,
            String ubicacion,
            LocalDateTime fechaPublicacion,
            EstadoPublicacion estado,
            Long autorId,
            String autorNombre,
            TipoUsuario autorTipo,
            boolean propia,
            Long cantidadPostulaciones,
            EstadoPostulacion miPostulacion,
            Long miPostulacionId,
            boolean puedePostularse) {
    }
}
