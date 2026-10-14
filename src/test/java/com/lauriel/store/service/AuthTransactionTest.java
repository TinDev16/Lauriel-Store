package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.lauriel.store.config.RoleInitializer;
import com.lauriel.store.dto.request.LoginRequest;
import com.lauriel.store.dto.request.RegisterRequest;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.repository.CustomerRepository;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.support.TestSchema;
import com.lauriel.store.support.TestSecrets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AuthTransactionTest {
    @Autowired private AuthService authService;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RoleInitializer initializer;
    @MockitoSpyBean private CustomerRepository customers;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) throws Exception {
        String url = TestSchema.createIsolatedDatabase();
        String secret = TestSecrets.randomBase64();
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.sql.init.mode", () -> "never");
        properties.add("app.jwt.secret", () -> secret);
        properties.add("app.jwt.expiration", () -> "PT1H");
        properties.add("app.roles.initialize", () -> "true");
    }

    @BeforeEach
    void resetSpy() {
        reset(customers);
    }

    @Test
    void registrationCommitsUserAndLinkedCustomerAndCanLogin() {
        String username = "customer_" + UUID.randomUUID();
        var response = authService.registerCustomer(
                new RegisterRequest(username, username + "@example.com", "TestPassword123", "Khách hàng", null));
        assertThat(response.role()).isEqualTo("CUSTOMER");
        var user = users.findByUsername(username).orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("TestPassword123");
        assertThat(passwordEncoder.matches("TestPassword123", user.getPasswordHash())).isTrue();
        assertThat(user.getRole().getRoleName()).isEqualTo("CUSTOMER");
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(customers.findAll()).anySatisfy(customer ->
                assertThat(customer.getUser().getUserId()).isEqualTo(user.getUserId()));
        var login = authService.login(new LoginRequest(username, "TestPassword123"));
        assertThat(login.userId()).isEqualTo(user.getUserId());
        assertThat(login.token()).isNotBlank();
    }

    @Test
    void customerFailureRollsBackAlreadyInsertedUser() {
        String username = "rollback_" + UUID.randomUUID();
        long userCount = users.count();
        long customerCount = customers.count();
        doThrow(new DataIntegrityViolationException("Simulated customer persistence failure"))
                .when(customers).saveAndFlush(any(Customer.class));
        assertThatThrownBy(() -> authService.registerCustomer(
                new RegisterRequest(username, null, "TestPassword123", "Rollback test", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(users.existsByUsername(username)).isFalse();
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(customers.count()).isEqualTo(customerCount);
    }

    @Test
    void roleInitializationIsIdempotentAndCreatesNoAccounts() {
        long usersBefore = users.count();
        var existingRoles = roles.findAll().stream().map(role -> role.getRoleId()).toList();
        initializer.run(new DefaultApplicationArguments());
        initializer.run(new DefaultApplicationArguments());
        assertThat(roles.findAll().stream().map(role -> role.getRoleId()).toList())
                .containsExactlyInAnyOrderElementsOf(existingRoles);
        assertThat(roles.findAll().stream().map(role -> role.getRoleName()).toList())
                .containsExactlyInAnyOrder("SUPER_ADMIN", "ADMIN", "STAFF", "CUSTOMER");
        assertThat(users.count()).isEqualTo(usersBefore);
    }
}
