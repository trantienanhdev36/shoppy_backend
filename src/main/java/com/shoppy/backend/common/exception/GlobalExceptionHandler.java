package com.shoppy.backend.common.exception;

import com.shoppy.backend.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus()) // Trả về HttpStatus (ví dụ: 400 BAD_REQUEST)
                .body(ApiResponse.error(errorCode.getMessage())); // Trả về JSON chuẩn
    }
}