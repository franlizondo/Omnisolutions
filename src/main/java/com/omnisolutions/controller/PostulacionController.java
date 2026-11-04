package com.omnisolutions.controller;

import com.omnisolutions.dto.PostulacionDtos.ContactoResponse;
import com.omnisolutions.dto.PostulacionDtos.PostulacionResponse;
import com.omnisolutions.model.EstadoPostulacion;
import com.omnisolutions.service.PostulacionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PostulacionController {

    private final PostulacionService postulacionService;

    public PostulacionController(PostulacionService postulacionService) {
        this.postulacionService = postulacionService;
    }

    @PostMapping("/publicaciones/{id}/postulaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public PostulacionResponse postular(@PathVariable Long id) {
        return postulacionService.postular(id);
    }

    @GetMapping("/postulaciones/realizadas")
    public List<PostulacionResponse> realizadas() {
        return postulacionService.realizadas();
    }

    @GetMapping("/postulaciones/recibidas")
    public List<PostulacionResponse> recibidas(@RequestParam(required = false) Long publicacionId) {
        return postulacionService.recibidas(publicacionId);
    }

    @PostMapping("/postulaciones/{id}/aceptar")
    public PostulacionResponse aceptar(@PathVariable Long id) {
        return postulacionService.resolver(id, EstadoPostulacion.ACEPTADA);
    }

    @PostMapping("/postulaciones/{id}/rechazar")
    public PostulacionResponse rechazar(@PathVariable Long id) {
        return postulacionService.resolver(id, EstadoPostulacion.RECHAZADA);
    }

    @GetMapping("/postulaciones/{id}/contacto")
    public ContactoResponse contacto(@PathVariable Long id) {
        return postulacionService.contacto(id);
    }
}
