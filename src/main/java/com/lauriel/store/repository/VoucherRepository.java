package com.lauriel.store.repository;

import com.lauriel.store.entity.Voucher;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface VoucherRepository extends JpaRepository<Voucher, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Voucher> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    @Query("select v from Voucher v where v.status = :status and v.quantity > 0 "
            + "and v.startDate <= :now and v.endDate >= :now order by v.endDate, v.voucherId")
    org.springframework.data.domain.Page<Voucher> findAvailable(
            com.lauriel.store.entity.enums.ActiveStatus status,
            java.time.LocalDateTime now, org.springframework.data.domain.Pageable pageable);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndVoucherIdNot(String code, Long voucherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where lower(v.code) = lower(:code)")
    Optional<Voucher> findByCodeForUpdate(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.voucherId = :voucherId")
    Optional<Voucher> findByIdForUpdate(Long voucherId);
}
