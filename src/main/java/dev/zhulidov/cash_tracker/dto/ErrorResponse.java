package dev.zhulidov.cash_tracker.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
@Builder
@Setter
@Getter
public class ErrorResponse{
    private int status;
    private String message;
    private String timestamp;
    private String errorCode;
    private Map<String,String> errors;

    public static ErrorResponse from(int status, String message, LocalDateTime timestamp, String errorCode){
        return new ErrorResponse(status,message,timestamp.format(DateTimeFormatter.ISO_DATE_TIME),errorCode);
    }
    public static ErrorResponse from(int status, String message, LocalDateTime timestamp, String errorCode, Map<String,String> errors){
        return new ErrorResponse(status,message, timestamp.format(DateTimeFormatter.ISO_DATE_TIME),errorCode,errors );
    }
    public ErrorResponse(int status, String message, String timestamp, String errorCode, Map<String, String> errors) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.errorCode = errorCode;
        this.errors = errors;
    }

    public ErrorResponse(int status, String message, String timestamp, String errorCode) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.errorCode = errorCode;
    }


}


