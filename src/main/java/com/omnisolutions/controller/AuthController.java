package com.omnisolutions.controller;

import com.omnisolutions.dto.UsuarioDtos.PerfilResponse;
import com.omnisolutions.dto.UsuarioDtos.RegistroRequest;
import com.omnisolutions.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

/**
 * Registro y token CSRF. El inicio y cierre de sesión los procesa Spring Security:
 * POST /api/auth/login (usuario, contrasena) y POST /api/auth/logout.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return usuarioService.registrar(req);
    }
}
