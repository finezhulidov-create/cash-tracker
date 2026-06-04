package dev.zhulidov.cash_tracker.controller;

import dev.zhulidov.cash_tracker.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.dto.UserDto;
import dev.zhulidov.cash_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;



    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable  Long id){
        return ResponseEntity.ok(service.getUserById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable  Long id){
        service.deleteUser(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

