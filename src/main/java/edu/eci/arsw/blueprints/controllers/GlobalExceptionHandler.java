package edu.eci.arsw.blueprints.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Traduce los errores de peticion invalida a 400 Bad Request usando el mismo
 * contrato ApiResponse<T> que devuelven los endpoints, en lugar del cuerpo de
 * error por defecto de Spring Boot.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Falla la validacion de @Valid sobre el cuerpo de la peticion. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        if (detail.isBlank()) detail = "invalid request body";
        return badRequest(detail);
    }

    /** El cuerpo no es JSON valido, viene vacio, o no calza con el tipo esperado. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return badRequest("malformed or missing JSON body");
    }

    /** Un parametro de ruta o de query no se puede convertir al tipo esperado. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("invalid value for parameter '" + ex.getName() + "'");
    }

    private ResponseEntity<ApiResponse<Object>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, message, null));
    }
}
