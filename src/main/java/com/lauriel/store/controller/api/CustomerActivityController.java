package com.lauriel.store.controller.api;

import com.lauriel.store.dto.request.CartSnapshotRequest;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.CustomerEngagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/activity")
public class CustomerActivityController {
    private final CustomerEngagementService service;

    public CustomerActivityController(CustomerEngagementService service) {
        this.service = service;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal StoreUserDetails user) {
        service.heartbeat(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cart")
    public ResponseEntity<Void> syncCart(@AuthenticationPrincipal StoreUserDetails user,
            @Valid @RequestBody CartSnapshotRequest request) {
        service.syncCart(user.getUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
