package com.omnisolutions.controller;

import com.omnisolutions.service.ApiException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Convierte las excepciones en respuestas JSON uniformes: {status, mensaje, errores}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> negocio(ApiException e) {
        return respuesta(e.getStatus(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> errores.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String primero = errores.values().stream().findFirst().orElse("Datos inválidos");
        return respuesta(HttpStatus.BAD_REQUEST, primero, errores);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> formato(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud tiene un formato o valor inválido", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException e) {
        return respuesta(HttpStatus.CONFLICT, "La operación no respeta las restricciones de los datos", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> acceso(AccessDeniedException e) {
        return respuesta(HttpStatus.FORBIDDEN, "No tenés permisos para realizar esta acción", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(NoResourceFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, "Recurso no encontrado", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodo(HttpRequestMethodNotSupportedException e) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> general(Exception e) {
        log.error("Error no controlado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno. Intentá nuevamente.", null);
    }

    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje,
                                                                 Map<String, String> errores) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("mensaje", mensaje);
        if (errores != null && !errores.isEmpty()) {
            body.put("errores", errores);
        }
        return ResponseEntity.status(status).body(body);
    }
}
