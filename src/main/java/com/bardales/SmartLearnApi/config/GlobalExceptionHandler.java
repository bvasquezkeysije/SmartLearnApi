package com.bardales.SmartLearnApi.config;

import com.bardales.SmartLearnApi.exception.BadRequestException;
import com.bardales.SmartLearnApi.exception.ForbiddenException;
import com.bardales.SmartLearnApi.exception.NotFoundException;
import com.bardales.SmartLearnApi.exception.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        Map<String, Object> extra = new LinkedHashMap<>();
        // Solo la capa de seguridad (JwtAuthenticationFilter/AuthenticationEntryPoint)
        // debe marcar authError=true. Las excepciones de negocio no deben disparar logout global.
        extra.put("authError", false);
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), extra);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation error");
        body.put("details", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        String rawMessage = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (rawMessage.contains("sqlstate") && rawMessage.contains("25006")
                || rawMessage.contains("read-only transaction")
                || rawMessage.contains("read only transaction")) {
            return build(HttpStatus.CONFLICT,
                    "Operacion no disponible temporalmente por modo de solo lectura. Intenta nuevamente.");
        }
        return build(HttpStatus.BAD_REQUEST, "No se pudo completar la operacion solicitada.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.startsWith("WRITE_IN_READONLY_TX:")) {
            return build(HttpStatus.CONFLICT, "Operacion bloqueada: intento de escritura en transaccion de solo lectura.");
        }
        return build(HttpStatus.BAD_REQUEST, "Estado de operacion invalido.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return build(status, message, null);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        if (extra != null && !extra.isEmpty()) {
            body.putAll(extra);
        }
        return ResponseEntity.status(status).body(body);
    }
}
