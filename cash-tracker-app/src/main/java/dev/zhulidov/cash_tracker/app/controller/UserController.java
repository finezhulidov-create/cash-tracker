package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.dto.UserDto;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;



    @GetMapping("/me")
    public ResponseEntity<UserDto> getUser(@AuthenticationPrincipal UserPrincipal principal){
        return ResponseEntity.ok(service.getUserById(principal.getId()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserPrincipal principal){
        service.deleteUser(principal.getId());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

