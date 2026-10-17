package com.lauriel.store.repository;

import com.lauriel.store.entity.CustomerAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomer_CustomerIdOrderByDefaultAddressDescAddressIdAsc(Long customerId);
}
