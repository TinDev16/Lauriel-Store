package com.lauriel.store.dto.store;

import java.util.List;

public record StoreAddressConfirmationResponse(
    String address,
    boolean saved,
    List<StoreCustomerAddress> addresses
) {}
