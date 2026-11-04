package com.omnisolutions.service;

import com.omnisolutions.dto.UsuarioDtos.PerfilRequest;
import com.omnisolutions.dto.UsuarioDtos.PerfilResponse;
import com.omnisolutions.dto.UsuarioDtos.RegistroRequest;
import com.omnisolutions.model.Usuario;
import com.omnisolutions.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Usado por Spring Security durante el inicio de sesión. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Usuario u = usuarioRepository.findByUsuario(normalizarUsuario(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));
        return User.withUsername(u.getUsuario())
                .password(u.getContrasena())
                .roles(u.getTipoUsuario().name())
                .build();
    }

    @Transactional
    public PerfilResponse registrar(RegistroRequest req) {
        String usuario = normalizarUsuario(req.usuario());
        if (req.contrasena().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.invalido("La contraseña es demasiado larga");
        }
        if (usuarioRepository.existsByUsuario(usuario)) {
            throw ApiException.conflicto("El nombre de usuario \"" + usuario + "\" ya está en uso");
        }
        Usuario u = new Usuario();
        u.setNombre(req.nombre().trim());
        u.setUsuario(usuario);
        u.setContrasena(passwordEncoder.encode(req.contrasena()));
        u.setTipoUsuario(req.tipoUsuario());
        u.setTelefono(textoOpcional(req.telefono()));
        try {
            return PerfilResponse.de(usuarioRepository.saveAndFlush(u));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflicto("El nombre de usuario \"" + usuario + "\" ya está en uso");
        }
    }

    @Transactional
    public PerfilResponse actualizarPerfil(PerfilRequest req) {
        Usuario u = actual();
        u.setNombre(req.nombre().trim());
        u.setTelefono(textoOpcional(req.telefono()));
        return PerfilResponse.de(usuarioRepository.save(u));
    }

    @Transactional(readOnly = true)
    public PerfilResponse perfilActual() {
        return PerfilResponse.de(actual());
    }

    @Transactional(readOnly = true)
    public PerfilResponse perfilDe(String usuario) {
        return usuarioRepository.findByUsuario(usuario).map(PerfilResponse::de)
                .orElseThrow(ApiException::noAutenticado);
    }

    /** Usuario autenticado en la sesión actual. */
    public Usuario actual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw ApiException.noAutenticado();
        }
        return usuarioRepository.findByUsuario(auth.getName()).orElseThrow(ApiException::noAutenticado);
    }

    static String normalizarUsuario(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase(Locale.ROOT);
    }

    /** Convierte cadenas vacías en null y recorta espacios. */
    static String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
