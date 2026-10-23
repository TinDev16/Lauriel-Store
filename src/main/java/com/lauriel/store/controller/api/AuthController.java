package com.lauriel.store.controller.api;

import com.lauriel.store.dto.request.LoginRequest;
import com.lauriel.store.dto.request.RegisterRequest;
import com.lauriel.store.dto.response.AuthUserResponse;
import com.lauriel.store.dto.response.LoginResponse;
import com.lauriel.store.dto.response.RegisterResponse;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
        @Valid @RequestBody RegisterRequest request
    ) {
        return authService.registerCustomer(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal StoreUserDetails user) {
        return new AuthUserResponse(
            user.getUserId(),
            user.getUsername(),
            user.getRole().name()
        );
    }
}
