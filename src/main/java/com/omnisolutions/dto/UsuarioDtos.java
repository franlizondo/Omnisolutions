package com.omnisolutions.dto;

import com.omnisolutions.model.TipoUsuario;
import com.omnisolutions.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Objetos de transferencia relacionados con usuarios. */
public final class UsuarioDtos {

    public static final String PATRON_TELEFONO = "^$|^[0-9+()\\s-]{6,20}$";
    public static final String MENSAJE_TELEFONO =
            "El teléfono debe tener entre 6 y 20 caracteres (números, espacios, +, -, paréntesis)";

    private UsuarioDtos() {
    }

    public record RegistroRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
            String nombre,

            @NotBlank(message = "El usuario es obligatorio")
            @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
            @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                     message = "El usuario solo puede contener letras, números, punto, guion y guion bajo")
            String usuario,

            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
            String contrasena,

            @NotNull(message = "El rol es obligatorio (EMPLEADOR o EMPLEADO)")
            TipoUsuario tipoUsuario,

            @Pattern(regexp = PATRON_TELEFONO, message = MENSAJE_TELEFONO)
            String telefono) {
    }

    /** Datos editables del perfil. El rol y el usuario no se pueden modificar. */
    public record PerfilRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
            String nombre,

            @Pattern(regexp = PATRON_TELEFONO, message = MENSAJE_TELEFONO)
            String telefono) {
    }

    /** Datos del propio usuario autenticado (incluye su teléfono). Nunca se usa para terceros. */
    public record PerfilResponse(Long id, String nombre, String usuario, TipoUsuario tipoUsuario,
                                 String telefono, LocalDateTime fechaRegistro) {

        public static PerfilResponse de(Usuario u) {
            return new PerfilResponse(u.getId(), u.getNombre(), u.getUsuario(), u.getTipoUsuario(),
                    u.getTelefono(), u.getFechaRegistro());
        }
    }
}
