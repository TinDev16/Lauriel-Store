package com.lauriel.store.dto.store;

public record StoreCustomerAddress(
    Long addressId,
    String address,
    boolean defaultAddress
) {}
