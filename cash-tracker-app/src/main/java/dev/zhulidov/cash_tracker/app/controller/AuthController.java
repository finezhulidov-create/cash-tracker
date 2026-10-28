package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.dto.LoginRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.dto.TokenResponse;
import dev.zhulidov.cash_tracker.app.service.UserService;
import dev.zhulidov.cash_tracker.app.util.Emails;
import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Auth", description = "Регистрация и вход. Лимит: 30 запросов в минуту с одного IP")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService service;

    @Operation(summary = "Login", description = "Вход по email и паролю. Возвращает JWT.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверный email или пароль (INVALID_CREDENTIALS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Превышен лимит запросов (RATE_LIMIT_EXCEEDED); см. заголовок Retry-After",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(Emails.normalize(request.email()), request.password())
        );
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(tokenFor(user.getEmail(), user.getId()));
    }

    @Operation(summary = "Register", description = "Регистрация. Сразу возвращает JWT, отдельный вход не нужен.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные: errors содержит сообщения по полям",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email уже зарегистрирован (EMAIL_ALREADY_EXISTS)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Превышен лимит запросов (RATE_LIMIT_EXCEEDED)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@RequestBody @Valid RegisterUserRequest request) {
        var user = service.createUser(request);
        return ResponseEntity.ok(tokenFor(user.email(), user.id()));
    }

    private TokenResponse tokenFor(String email, Long userId) {
        return new TokenResponse(jwtService.generateToken(email, userId), TOKEN_TYPE, jwtService.getExpirationSeconds());
    }

}
