package com.omnisolutions.controller;

import com.omnisolutions.dto.UsuarioDtos.PerfilRequest;
import com.omnisolutions.dto.UsuarioDtos.PerfilResponse;
import com.omnisolutions.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public PerfilResponse perfil() {
        return usuarioService.perfilActual();
    }

    @PutMapping("/me")
    public PerfilResponse actualizar(@Valid @RequestBody PerfilRequest req) {
        return usuarioService.actualizarPerfil(req);
    }
}
