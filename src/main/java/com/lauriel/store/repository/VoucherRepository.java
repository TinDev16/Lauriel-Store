package com.lauriel.store.repository;

import com.lauriel.store.entity.Voucher;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndVoucherIdNot(String code, Long voucherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where lower(v.code) = lower(:code)")
    Optional<Voucher> findByCodeForUpdate(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.voucherId = :voucherId")
    Optional<Voucher> findByIdForUpdate(Long voucherId);
}
