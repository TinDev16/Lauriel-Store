package com.lauriel.store.service;

import com.lauriel.store.dto.request.CreateManagementAccountRequest;
import com.lauriel.store.dto.request.ResetPasswordRequest;
import com.lauriel.store.dto.request.UpdateAccountRequest;
import com.lauriel.store.dto.response.AccountDetailResponse;
import com.lauriel.store.dto.response.AccountSummaryResponse;
import com.lauriel.store.dto.response.CustomerAccountProfileResponse;
import com.lauriel.store.dto.response.PageResponse;
import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.exception.ResourceConflictException;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.CustomerRepository;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.repository.UserSpecifications;
import com.lauriel.store.security.StoreRole;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountManagementService {

    private static final Logger log = LoggerFactory.getLogger(
        AccountManagementService.class
    );
    private static final String SUPER_ADMIN = StoreRole.SUPER_ADMIN.name();

    private final UserRepository users;
    private final RoleRepository roles;
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;

    public AccountManagementService(
        UserRepository users,
        RoleRepository roles,
        CustomerRepository customers,
        PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.roles = roles;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountSummaryResponse> list(
        int page,
        int size,
        String keyword,
        StoreRole role,
        UserStatus status
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = users.findAll(
            UserSpecifications.filter(keyword, role, status),
            PageRequest.of(
                safePage,
                safeSize,
                Sort.by("status")
                    .ascending()
                    .and(Sort.by("createdAt").descending())
                    .and(Sort.by("userId").descending())
            )
        );
        return new PageResponse<>(
            result
                .getContent()
                .stream()
                .map(AccountManagementService::summary)
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AccountDetailResponse get(Long id) {
        User user = find(id);
        var customer = customers
            .findByUser_UserId(id)
            .map(value ->
                new CustomerAccountProfileResponse(
                    value.getCustomerId(),
                    value.getCustomerName(),
                    value.getPhone(),
                    value.getAddress(),
                    value.getCreatedAt()
                )
            )
            .orElse(null);
        StoreRole role = role(user);
        return new AccountDetailResponse(
            user.getUserId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            role,
            user.getStatus(),
            user.getCreatedAt(),
            customer
        );
    }

    @Transactional
    public AccountDetailResponse create(
        Long actorId,
        CreateManagementAccountRequest request
    ) {
        requireSuperAdmin(actorId);
        if (
            request.role() != StoreRole.ADMIN &&
            request.role() != StoreRole.STAFF
        ) {
            throw new BadRequestException(
                "Chỉ được tạo tài khoản ADMIN hoặc STAFF từ màn hình quản trị."
            );
        }
        validatePassword(request.password());
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp.");
        }
        String username = request.username().strip();
        String email = optional(request.email());
        checkUnique(username, email, null);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().strip());
        user.setEmail(email);
        user.setPhone(optional(request.phone()));
        user.setRole(
            roles
                .findByRoleName(request.role().name())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Thiếu role " +
                            request.role().name() +
                            " trong database"
                    )
                )
        );
        user.setStatus(request.status());
        user = users.saveAndFlush(user);
        log.info(
            "SUPER_ADMIN {} created {} account {}",
            actorId,
            request.role(),
            user.getUserId()
        );
        return get(user.getUserId());
    }

    @Transactional
    public AccountDetailResponse update(
        Long actorId,
        Long targetId,
        UpdateAccountRequest request
    ) {
        requireSuperAdmin(actorId);
        User target = find(targetId);
        StoreRole currentRole = role(target);
        validateRoleChange(currentRole, request.role());
        protectLastSuperAdmin(target, request.role(), request.status());
        String email = optional(request.email());
        checkUnique(target.getUsername(), email, targetId);

        target.setFullName(request.fullName().strip());
        target.setEmail(email);
        target.setPhone(optional(request.phone()));
        target.setStatus(request.status());
        if (currentRole != request.role()) {
            target.setRole(
                roles
                    .findByRoleName(request.role().name())
                    .orElseThrow(() ->
                        new IllegalStateException(
                            "Thiếu role " +
                                request.role().name() +
                                " trong database"
                        )
                    )
            );
        }
        users.saveAndFlush(target);
        log.info(
            "SUPER_ADMIN {} updated USER {} role {} -> {}, status {}",
            actorId,
            targetId,
            currentRole,
            request.role(),
            request.status()
        );
        return get(targetId);
    }

    @Transactional
    public void changeRole(Long actorId, Long targetId, StoreRole newRole) {
        requireSuperAdmin(actorId);
        User target = find(targetId);
        StoreRole currentRole = role(target);
        validateRoleChange(currentRole, newRole);
        protectLastSuperAdmin(target, newRole, target.getStatus());
        if (currentRole != newRole) {
            target.setRole(
                roles
                    .findByRoleName(newRole.name())
                    .orElseThrow(() ->
                        new IllegalStateException(
                            "Thiếu role " + newRole.name() + " trong database"
                        )
                    )
            );
            users.saveAndFlush(target);
            log.info(
                "SUPER_ADMIN {} changed USER {} role {} -> {}",
                actorId,
                targetId,
                currentRole,
                newRole
            );
        }
    }

    @Transactional
    public void setStatus(Long actorId, Long targetId, UserStatus status) {
        requireSuperAdmin(actorId);
        User target = find(targetId);
        protectLastSuperAdmin(target, role(target), status);
        UserStatus previous = target.getStatus();
        target.setStatus(status);
        users.saveAndFlush(target);
        log.info(
            "SUPER_ADMIN {} changed USER {} status {} -> {}",
            actorId,
            targetId,
            previous,
            status
        );
    }

    @Transactional
    public void resetPassword(
        Long actorId,
        Long targetId,
        ResetPasswordRequest request
    ) {
        requireSuperAdmin(actorId);
        User target = find(targetId);
        StoreRole targetRole = role(target);
        if (targetRole != StoreRole.ADMIN && targetRole != StoreRole.STAFF) {
            throw new BadRequestException(
                "Chỉ hỗ trợ đặt lại mật khẩu cho ADMIN hoặc STAFF."
            );
        }
        validatePassword(request.password());
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp.");
        }
        target.setPasswordHash(passwordEncoder.encode(request.password()));
        users.saveAndFlush(target);
        log.info(
            "SUPER_ADMIN {} reset password for USER {}",
            actorId,
            targetId
        );
    }

    private User requireSuperAdmin(Long actorId) {
        User actor = find(actorId);
        if (
            actor.getStatus() != UserStatus.ACTIVE ||
            role(actor) != StoreRole.SUPER_ADMIN
        ) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Bạn không có quyền quản lý tài khoản."
            );
        }
        return actor;
    }

    private void validateRoleChange(StoreRole currentRole, StoreRole newRole) {
        if (
            currentRole == StoreRole.CUSTOMER && newRole != StoreRole.CUSTOMER
        ) {
            throw new BadRequestException(
                "Không thể đổi role CUSTOMER vì hồ sơ khách hàng đang được liên kết."
            );
        }
        if (
            currentRole != StoreRole.CUSTOMER && newRole == StoreRole.CUSTOMER
        ) {
            throw new BadRequestException(
                "Không thể đổi tài khoản quản lý thành CUSTOMER."
            );
        }
        if (
            currentRole != StoreRole.SUPER_ADMIN &&
            newRole == StoreRole.SUPER_ADMIN
        ) {
            throw new BadRequestException(
                "Không cấp role SUPER_ADMIN từ giao diện quản trị."
            );
        }
    }

    private void protectLastSuperAdmin(
        User target,
        StoreRole newRole,
        UserStatus newStatus
    ) {
        if (
            role(target) == StoreRole.SUPER_ADMIN &&
            target.getStatus() == UserStatus.ACTIVE &&
            (newRole != StoreRole.SUPER_ADMIN ||
                newStatus != UserStatus.ACTIVE) &&
            users.countByRole_RoleNameAndStatus(
                SUPER_ADMIN,
                UserStatus.ACTIVE
            ) <= 1
        ) {
            throw new ResourceConflictException(
                "Không thể khóa, ngừng hoạt động hoặc hạ quyền SUPER_ADMIN cuối cùng."
            );
        }
    }

    private void checkUnique(String username, String email, Long currentId) {
        if (currentId == null && users.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Tên đăng nhập đã tồn tại.");
        }
        if (
            email != null &&
            (currentId == null
                ? users.existsByEmailIgnoreCase(email)
                : users.existsByEmailIgnoreCaseAndUserIdNot(email, currentId))
        ) {
            throw new DuplicateResourceException("Email đã được sử dụng.");
        }
    }

    private User find(Long id) {
        return users
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy tài khoản.")
            );
    }

    private static StoreRole role(User user) {
        return StoreRole.valueOf(
            user.getRole().getRoleName().toUpperCase(Locale.ROOT)
        );
    }

    private static AccountSummaryResponse summary(User user) {
        return new AccountSummaryResponse(
            user.getUserId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            role(user),
            user.getStatus(),
            user.getCreatedAt()
        );
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void validatePassword(String password) {
        if (
            password == null ||
            password.isBlank() ||
            password.length() < 8 ||
            password.length() > 72
        ) {
            throw new BadRequestException(
                "Mật khẩu phải có từ 8 đến 72 ký tự."
            );
        }
    }
}
