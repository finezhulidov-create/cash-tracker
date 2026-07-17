package dev.zhulidov.cash_tracker.notifications.controller;

import dev.zhulidov.cash_tracker.notifications.dto.RegistrationVerificationDto;
import dev.zhulidov.cash_tracker.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/send")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(
            summary = "Send Email",
            description = "Sends an email message based on the operation specified in the request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email successfully sended"),
            @ApiResponse(responseCode = "400", description = "Not valid data in request")
    })
    @PostMapping
    public ResponseEntity<Void> sendEmail(@RequestBody @Valid RegistrationVerificationDto dto ){
            notificationService.sendByOperation(dto.email(), dto.operations());
            return ResponseEntity.status(HttpStatus.OK).build();
    }
}
