package br.edu.ifrn.promptuario.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiErrorResponse> validation(Exception exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException validationException) {
            for (FieldError fieldError : validationException.getBindingResult().getFieldErrors()) {
                String message = fieldError.getDefaultMessage() == null
                        ? "Valor inválido"
                        : fieldError.getDefaultMessage();
                fieldErrors.putIfAbsent(fieldError.getField(), message);
            }
        } else if (exception instanceof ConstraintViolationException validationException) {
            for (ConstraintViolation<?> violation : validationException.getConstraintViolations()) {
                String path = violation.getPropertyPath().toString();
                String field = path.substring(path.lastIndexOf('.') + 1);
                fieldErrors.putIfAbsent(field, violation.getMessage());
            }
        }
        return error(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos", request, fieldErrors);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> conflict(ConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> forbidden(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Acesso negado", request, Map.of());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> business(BusinessException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> badRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> unreadableBody(HttpMessageNotReadableException exception,
                                                             HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
