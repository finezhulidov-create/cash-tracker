package dev.zhulidov.cash_tracker.exception;

import dev.zhulidov.cash_tracker.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex){
        ErrorResponse body = ErrorResponse.from(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now(), ex.getDetailMessageCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e){
        ErrorResponse body = ErrorResponse.from(HttpStatus.NOT_FOUND.value(), e.getMessage(), LocalDateTime.now(),"RESOURCE_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InaccessibleResourceException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(InaccessibleResourceException ex){
        ErrorResponse body = ErrorResponse.from(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now(),"ACCESS_DENIED");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ConstraintViolationException exception){
        Map<String,String> error = new HashMap<>();
        exception.getConstraintViolations()
                .forEach(err-> error.put(err.getPropertyPath().toString().substring(err.getPropertyPath().toString().lastIndexOf(".")+1),err.getMessage()));
        ErrorResponse body = ErrorResponse.from(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), LocalDateTime.now(),"VALIDATION_ERROR", error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);

    }
}
