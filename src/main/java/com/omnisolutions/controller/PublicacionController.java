package com.omnisolutions.controller;

import com.omnisolutions.dto.PublicacionDtos.PublicacionRequest;
import com.omnisolutions.dto.PublicacionDtos.PublicacionResponse;
import com.omnisolutions.model.TipoPublicacion;
import com.omnisolutions.service.PublicacionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    /** Feed de publicaciones activas. Filtros opcionales: tipo (TRABAJO/SERVICIO) y texto. */
    @GetMapping
    public List<PublicacionResponse> feed(@RequestParam(required = false) TipoPublicacion tipo,
                                          @RequestParam(required = false) String q) {
        return publicacionService.feed(tipo, q);
    }

    @GetMapping("/mias")
    public List<PublicacionResponse> mias() {
        return publicacionService.mias();
    }

    @GetMapping("/{id}")
    public PublicacionResponse detalle(@PathVariable Long id) {
        return publicacionService.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacionResponse crear(@Valid @RequestBody PublicacionRequest req) {
        return publicacionService.crear(req);
    }

    @PutMapping("/{id}")
    public PublicacionResponse modificar(@PathVariable Long id, @Valid @RequestBody PublicacionRequest req) {
        return publicacionService.modificar(id, req);
    }

    @PostMapping("/{id}/cerrar")
    public PublicacionResponse cerrar(@PathVariable Long id) {
        return publicacionService.cerrar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        publicacionService.eliminar(id);
    }
}
