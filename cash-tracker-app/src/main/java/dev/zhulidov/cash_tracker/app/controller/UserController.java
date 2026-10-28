package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.dto.BalanceResponse;
import dev.zhulidov.cash_tracker.app.dto.UpdateBalanceRequest;
import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.common.dto.ErrorResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Профиль текущего пользователя")
@Validated
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @Operation(summary = "Current user profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль"),
            @ApiResponse(responseCode = "401", description = "Нет или невалидный токен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(service.getUserById(principal.getId()));
    }

    @Operation(summary = "Delete account",
            description = "Удаляет пользователя вместе со всеми его категориями, транзакциями и бюджетами.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Аккаунт удалён"),
            @ApiResponse(responseCode = "401", description = "Нет или невалидный токен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserPrincipal principal){
        service.deleteUser(principal.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Set balance",
            description = "Баланс задаётся вручную и не меняется от трат.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс обновлён"),
            @ApiResponse(responseCode = "400", description = "Баланс отсутствует или отрицательный",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Нет или невалидный токен",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/me/balance")
    public ResponseEntity<BalanceResponse> updateBalance(@AuthenticationPrincipal UserPrincipal me,
                                                         @RequestBody @Valid UpdateBalanceRequest request){
        return ResponseEntity.ok(new BalanceResponse(service.updateUserBalance(me.getId(), request.balance())));
    }

}
