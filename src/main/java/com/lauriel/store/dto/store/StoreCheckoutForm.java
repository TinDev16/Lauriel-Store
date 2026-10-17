package com.lauriel.store.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreCheckoutForm(@NotBlank @Size(max = 10000) String shippingAddress,
        @Size(max = 50) String voucherCode, String mode, Boolean saveAddress, Boolean setAsDefault) {}
