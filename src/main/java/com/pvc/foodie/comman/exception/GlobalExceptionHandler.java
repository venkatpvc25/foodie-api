
package com.pvc.foodie.comman.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import com.pvc.foodie.comman.response.ApiError;

import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
                log.warn("Business exception: code={}, message={}", ex.getErrorCode(), ex.getMessage(), ex);

                return ResponseEntity
                                .badRequest()
                                .body(new ApiError(
                                                false,
                                                ex.getErrorCode().name(),
                                                ex.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {

                String errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                log.warn("Validation exception: {}", errors, ex);

                return ResponseEntity
                                .badRequest()
                                .body(new ApiError(
                                                false,
                                                ErrorCode.VALIDATION_ERROR.name(),
                                                errors));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleGeneral(Exception ex) {
                log.error("Unhandled exception", ex);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ApiError(
                                                false,
                                                ErrorCode.INTERNAL_ERROR.name(),
                                                "Something went wrong"));
        }
}
