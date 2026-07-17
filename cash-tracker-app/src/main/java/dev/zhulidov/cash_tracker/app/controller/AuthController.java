package dev.zhulidov.cash_tracker.app.controller;

import dev.zhulidov.cash_tracker.app.dto.LoginRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserRequest;
import dev.zhulidov.cash_tracker.app.dto.RegisterUserResponse;
import dev.zhulidov.cash_tracker.common.security.UserPrincipal;
import dev.zhulidov.cash_tracker.app.service.CustomUserDetailsService;
import dev.zhulidov.cash_tracker.common.service.JwtService;
import dev.zhulidov.cash_tracker.app.service.UserService;
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

    @PostMapping("/v1/login")
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

