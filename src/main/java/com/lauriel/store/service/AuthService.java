package com.lauriel.store.service;

import com.lauriel.store.dto.request.LoginRequest;
import com.lauriel.store.dto.request.RegisterRequest;
import com.lauriel.store.dto.response.LoginResponse;
import com.lauriel.store.dto.response.RegisterResponse;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.repository.CustomerRepository;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.security.JwtService;
import com.lauriel.store.security.StoreRole;
import com.lauriel.store.security.StoreUserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        CustomerRepository customerRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse registerCustomer(RegisterRequest request) {
        String username = request.username().strip();
        String email = optionalText(request.email());
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Tên đăng nhập đã tồn tại");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email đã được sử dụng");
        }
        var role = roleRepository
            .findByRoleName(StoreRole.CUSTOMER.name())
            .orElseThrow(() ->
                new IllegalStateException("Thiếu role CUSTOMER trong database")
            );
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().strip());
        user.setPhone(optionalText(request.phone()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.saveAndFlush(user);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCustomerName(user.getFullName());
        customer.setPhone(user.getPhone());
        customerRepository.saveAndFlush(customer);
        return new RegisterResponse(
            user.getUserId(),
            user.getUsername(),
            user.getEmail(),
            StoreRole.CUSTOMER.name(),
            "Đăng ký thành công"
        );
    }

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username().strip(),
                request.password()
            )
        );
        return jwtService.issue(
            (StoreUserDetails) authentication.getPrincipal()
        );
    }

    private String optionalText(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
