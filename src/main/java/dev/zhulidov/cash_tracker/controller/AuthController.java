package dev.zhulidov.cash_tracker.controller;

import dev.zhulidov.cash_tracker.dto.LoginRequest;
import dev.zhulidov.cash_tracker.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.model.User;
import dev.zhulidov.cash_tracker.model.UserPrincipal;
import dev.zhulidov.cash_tracker.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.service.JwtService;
import dev.zhulidov.cash_tracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    private final UserService service;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginRequest request) {
     var auth =   authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(),request.password())
        );
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();
        return ResponseEntity.ok(jwtService.generateToken(request.email(),user.getId()));

    }
    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> createUser(@RequestBody @Valid RegisterUserRequest request){

        return ResponseEntity.ok(service.createUser(request));
    }

}

