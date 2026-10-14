package com.lauriel.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.dto.request.CreateManagementAccountRequest;
import com.lauriel.store.dto.request.ResetPasswordRequest;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.exception.DuplicateResourceException;
import com.lauriel.store.exception.ResourceConflictException;
import com.lauriel.store.repository.CustomerRepository;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.security.StoreRole;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.AccountManagementService;
import com.lauriel.store.support.TestSchema;
import com.lauriel.store.support.TestSecrets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SuperAdminAccountManagementTest {
    private static final String OLD_PASSWORD = "OldPassword@123";
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CustomerRepository customers;
    @Autowired PasswordEncoder encoder;
    @Autowired AccountManagementService accounts;
    private final Map<StoreRole, User> fixtures = new HashMap<>();
    private MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) throws Exception {
        String databaseUrl = TestSchema.createIsolatedDatabase();
        properties.add("spring.datasource.url", () -> databaseUrl);
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("app.jwt.secret", TestSecrets::randomBase64);
        properties.add("telegram.enabled", () -> "false");
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        customers.deleteAllInBatch();
        users.deleteAllInBatch();
        fixtures.clear();
        for (StoreRole role : StoreRole.values()) {
            User user = new User();
            user.setUsername("account-" + role.name().toLowerCase());
            user.setFullName("Fixture " + role.name());
            user.setEmail(role.name().toLowerCase() + "@example.com");
            user.setPasswordHash(encoder.encode(OLD_PASSWORD));
            user.setRole(roles.findByRoleName(role.name()).orElseThrow());
            user.setStatus(UserStatus.ACTIVE);
            fixtures.put(role, users.saveAndFlush(user));
        }
        Customer customer = new Customer();
        customer.setUser(fixtures.get(StoreRole.CUSTOMER));
        customer.setCustomerName("Khách hàng giữ dữ liệu");
        customer.setAddress("Địa chỉ lịch sử");
        customers.saveAndFlush(customer);
    }

    @Test
    void onlySuperAdminCanOpenAccountManagementAndCsrfProtectsMutations() throws Exception {
        mvc.perform(get("/admin/accounts")).andExpect(status().is3xxRedirection());
        for (StoreRole denied : new StoreRole[] {StoreRole.CUSTOMER, StoreRole.STAFF, StoreRole.ADMIN}) {
            mvc.perform(get("/admin/accounts").with(user(details(denied)))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/accounts").with(superAdmin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý tài khoản")))
                .andExpect(content().string(containsString("account-admin")));
        Long adminId = fixtures.get(StoreRole.ADMIN).getUserId();
        mvc.perform(get("/admin/accounts/" + adminId).with(superAdmin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Đặt lại mật khẩu")));
        mvc.perform(get("/admin/accounts/new").with(superAdmin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Tạo tài khoản quản lý")));
        mvc.perform(post("/admin/accounts").with(superAdmin())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/accounts").with(superAdmin()).with(csrf())
                        .param("username", "web-created-staff")
                        .param("password", "WebPassword@123")
                        .param("confirmPassword", "WebPassword@123")
                        .param("fullName", "Nhân viên từ web")
                        .param("email", "web-created@example.com")
                        .param("phone", "0900000001")
                        .param("role", "STAFF").param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection());
        assertThat(users.findByUsername("web-created-staff")).isPresent();
    }

    @Test
    void superAdminCreatesStaffAndAdminAndDuplicatesAreRejected() {
        Long actor = fixtures.get(StoreRole.SUPER_ADMIN).getUserId();
        var staff = accounts.create(actor, request("new-staff", "new-staff@example.com", StoreRole.STAFF));
        var admin = accounts.create(actor, request("new-admin", "new-admin@example.com", StoreRole.ADMIN));
        assertThat(staff.role()).isEqualTo(StoreRole.STAFF);
        assertThat(admin.role()).isEqualTo(StoreRole.ADMIN);
        assertThat(encoder.matches("NewPassword@123", users.findById(staff.userId()).orElseThrow().getPasswordHash())).isTrue();
        assertThatThrownBy(() -> accounts.create(actor, request("new-staff", "unique@example.com", StoreRole.STAFF)))
                .isInstanceOf(DuplicateResourceException.class);
        assertThatThrownBy(() -> accounts.create(actor, request("unique-user", "new-admin@example.com", StoreRole.ADMIN)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void lockUnlockAndPasswordResetAffectLoginImmediately() throws Exception {
        User staff = fixtures.get(StoreRole.STAFF);
        Long actor = fixtures.get(StoreRole.SUPER_ADMIN).getUserId();
        accounts.setStatus(actor, staff.getUserId(), UserStatus.LOCKED);
        mvc.perform(formLogin().user(staff.getUsername()).password(OLD_PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
        accounts.setStatus(actor, staff.getUserId(), UserStatus.ACTIVE);
        mvc.perform(formLogin().user(staff.getUsername()).password(OLD_PASSWORD))
                .andExpect(redirectedUrl("/admin"));

        String oldHash = users.findById(staff.getUserId()).orElseThrow().getPasswordHash();
        accounts.resetPassword(actor, staff.getUserId(), new ResetPasswordRequest("NewPassword@456", "NewPassword@456"));
        String newHash = users.findById(staff.getUserId()).orElseThrow().getPasswordHash();
        assertThat(newHash).isNotEqualTo(oldHash);
        mvc.perform(formLogin().user(staff.getUsername()).password(OLD_PASSWORD))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(formLogin().user(staff.getUsername()).password("NewPassword@456"))
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void roleChangeRefreshesExistingWebSessionOnNextRequest() throws Exception {
        User staff = fixtures.get(StoreRole.STAFF);
        var login = mvc.perform(formLogin().user(staff.getUsername()).password(OLD_PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/admin/vouchers").session(session)).andExpect(status().isForbidden());

        accounts.changeRole(fixtures.get(StoreRole.SUPER_ADMIN).getUserId(), staff.getUserId(), StoreRole.ADMIN);
        mvc.perform(get("/admin/vouchers").session(session)).andExpect(status().isOk());
        assertThat(users.findById(staff.getUserId()).orElseThrow().getRole().getRoleName()).isEqualTo("ADMIN");
    }

    @Test
    void lastSuperAdminCannotBeLockedDeactivatedOrDemoted() {
        Long ownerId = fixtures.get(StoreRole.SUPER_ADMIN).getUserId();
        assertThatThrownBy(() -> accounts.setStatus(ownerId, ownerId, UserStatus.LOCKED))
                .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> accounts.setStatus(ownerId, ownerId, UserStatus.INACTIVE))
                .isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> accounts.changeRole(ownerId, ownerId, StoreRole.ADMIN))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(users.findById(ownerId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(users.findById(ownerId).orElseThrow().getRole().getRoleName()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void lockingCustomerKeepsCustomerProfile() {
        Long actor = fixtures.get(StoreRole.SUPER_ADMIN).getUserId();
        Long customerUserId = fixtures.get(StoreRole.CUSTOMER).getUserId();
        Long profileId = customers.findByUser_UserId(customerUserId).orElseThrow().getCustomerId();
        accounts.setStatus(actor, customerUserId, UserStatus.LOCKED);
        assertThat(customers.findById(profileId)).isPresent();
        assertThat(accounts.get(customerUserId).customerProfile()).isNotNull();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor superAdmin() {
        return user(details(StoreRole.SUPER_ADMIN));
    }

    private StoreUserDetails details(StoreRole role) {
        return new StoreUserDetails(users.findById(fixtures.get(role).getUserId()).orElseThrow());
    }

    private CreateManagementAccountRequest request(String username, String email, StoreRole role) {
        return new CreateManagementAccountRequest(username, "NewPassword@123", "NewPassword@123",
                "Người quản lý mới", email, "0900000000", role, UserStatus.ACTIVE);
    }
}
