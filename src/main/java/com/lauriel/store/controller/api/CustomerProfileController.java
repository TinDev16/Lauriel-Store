package com.lauriel.store.controller.api;

import com.lauriel.store.dto.request.ProfileUpdateRequest;
import com.lauriel.store.dto.response.ProfileResponse;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerProfileController {

    private final CustomerService customers;

    public CustomerProfileController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping("/me")
    public ProfileResponse me(@AuthenticationPrincipal StoreUserDetails user) {
        return customers.me(user.getUserId());
    }

    @PutMapping("/me")
    public ProfileResponse update(
        @AuthenticationPrincipal StoreUserDetails user,
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return customers.updateMe(user.getUserId(), request);
    }
}
