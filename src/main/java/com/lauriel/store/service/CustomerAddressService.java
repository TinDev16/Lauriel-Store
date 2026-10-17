package com.lauriel.store.service;

import com.lauriel.store.dto.store.StoreCustomerAddress;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.CustomerAddress;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.CustomerAddressRepository;
import com.lauriel.store.repository.CustomerRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerAddressService {
    private final CustomerAddressRepository addresses;
    private final CustomerRepository customers;

    public CustomerAddressService(CustomerAddressRepository addresses, CustomerRepository customers) {
        this.addresses = addresses;
        this.customers = customers;
    }

    @Transactional
    public List<StoreCustomerAddress> listForCheckout(Long userId) {
        Customer customer = requireCustomer(userId);
        List<CustomerAddress> saved = new ArrayList<>(addresses
                .findByCustomer_CustomerIdOrderByDefaultAddressDescAddressIdAsc(customer.getCustomerId()));
        if (saved.isEmpty() && customer.getAddress() != null && !customer.getAddress().isBlank()) {
            CustomerAddress legacyAddress = new CustomerAddress();
            legacyAddress.setCustomer(customer);
            legacyAddress.setAddressText(customer.getAddress().strip());
            legacyAddress.setDefaultAddress(true);
            addresses.saveAndFlush(legacyAddress);
            saved.add(legacyAddress);
        }
        synchronizeProfileDefault(customer, saved);
        ensureSingleDefault(customer, saved);
        return saved.stream().map(CustomerAddressService::response).toList();
    }

    @Transactional
    public void saveFromCheckout(Long userId, String addressText, boolean makeDefault) {
        Customer customer = requireCustomer(userId);
        String normalized = addressText.strip();
        List<CustomerAddress> saved = new ArrayList<>(addresses
                .findByCustomer_CustomerIdOrderByDefaultAddressDescAddressIdAsc(customer.getCustomerId()));
        CustomerAddress selected = saved.stream()
                .filter(address -> sameAddress(address.getAddressText(), normalized))
                .findFirst()
                .orElse(null);

        if (selected == null) {
            selected = new CustomerAddress();
            selected.setCustomer(customer);
            selected.setAddressText(normalized);
            selected.setDefaultAddress(saved.isEmpty() || makeDefault);
            saved.add(selected);
        } else if (makeDefault) {
            selected.setDefaultAddress(true);
        }

        if (selected.isDefaultAddress()) {
            for (CustomerAddress address : saved) {
                if (address != selected) address.setDefaultAddress(false);
            }
            customer.setAddress(normalized);
            customers.save(customer);
        }
        addresses.saveAll(saved);
        addresses.flush();
    }

    private void synchronizeProfileDefault(Customer customer, List<CustomerAddress> saved) {
        if (customer.getAddress() == null || customer.getAddress().isBlank() || saved.isEmpty()) return;
        String profileAddress = customer.getAddress().strip();
        CustomerAddress match = saved.stream()
                .filter(address -> sameAddress(address.getAddressText(), profileAddress))
                .findFirst()
                .orElse(null);
        if (match == null) {
            match = new CustomerAddress();
            match.setCustomer(customer);
            match.setAddressText(profileAddress);
            saved.add(match);
        }
        for (CustomerAddress address : saved) address.setDefaultAddress(address == match);
        addresses.saveAll(saved);
    }

    private void ensureSingleDefault(Customer customer, List<CustomerAddress> saved) {
        if (saved.isEmpty()) return;
        CustomerAddress selected = saved.stream().filter(CustomerAddress::isDefaultAddress).findFirst()
                .orElse(saved.getFirst());
        boolean changed = false;
        for (CustomerAddress address : saved) {
            boolean shouldBeDefault = address == selected;
            if (address.isDefaultAddress() != shouldBeDefault) {
                address.setDefaultAddress(shouldBeDefault);
                changed = true;
            }
        }
        if (!sameAddress(customer.getAddress(), selected.getAddressText())) {
            customer.setAddress(selected.getAddressText());
            customers.save(customer);
        }
        if (changed) addresses.saveAll(saved);
    }

    private Customer requireCustomer(Long userId) {
        return customers.findByUser_UserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khách hàng"));
    }

    private static boolean sameAddress(String first, String second) {
        return first != null && first.strip().equalsIgnoreCase(second);
    }

    private static StoreCustomerAddress response(CustomerAddress address) {
        return new StoreCustomerAddress(address.getAddressId(), address.getAddressText(), address.isDefaultAddress());
    }
}
