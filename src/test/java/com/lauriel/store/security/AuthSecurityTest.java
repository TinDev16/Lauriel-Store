package com.lauriel.store.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.config.AuthenticationConfig;
import com.lauriel.store.config.JwtConfig;
import com.lauriel.store.config.SecurityConfig;
import com.lauriel.store.controller.api.AuthController;
import com.lauriel.store.dto.response.LoginResponse;
import com.lauriel.store.entity.Customer;
import com.lauriel.store.entity.Role;
import com.lauriel.store.entity.User;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.exception.GlobalExceptionHandler;
import com.lauriel.store.repository.CustomerRepository;
import com.lauriel.store.repository.RoleRepository;
import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.service.AuthService;
import com.lauriel.store.support.TestSecrets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(AuthSecurityTest.TestConfig.class)
@WebAppConfiguration
class AuthSecurityTest {
    private static final String PASSWORD = "TestPassword123";
    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private RoleRepository roles;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private JwtEncoder encoder;
    @Autowired private JwtDecoder decoder;
    @Autowired private ObjectMapper mapper;
    private MockMvc mvc;
    private User account;
    private Role customerRole;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        String secret = TestSecrets.randomBase64();
        properties.add("app.jwt.secret", () -> secret);
        properties.add("app.jwt.expiration", () -> "PT1H");
    }

    @BeforeEach
    void setUp() {
        reset(users, customers, roles);
        customerRole = new Role();
        customerRole.setRoleId(77L); // IDs are deliberately unrelated to any assumed seed order.
        customerRole.setRoleName("CUSTOMER");
        account = new User();
        account.setUserId(123L);
        account.setUsername("customer01");
        account.setPasswordHash(passwordEncoder.encode(PASSWORD));
        account.setRole(customerRole);
        account.setStatus(UserStatus.ACTIVE);
        when(users.findByUsername("customer01")).thenAnswer(invocation -> Optional.of(account));
        when(users.findById(123L)).thenAnswer(invocation -> Optional.of(account));
        when(roles.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User created = invocation.getArgument(0);
            created.setUserId(321L);
            return created;
        });
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private String registerJson() {
        return "{\"username\":\"newcustomer\",\"email\":\"new@example.com\","
                + "\"password\":\"" + PASSWORD + "\",\"fullName\":\"Khách hàng\",\"phone\":\"0901234567\"}";
    }

    private String token() {
        return jwtService.issue(new StoreUserDetails(account)).token();
    }

    private String signedToken(Instant issuedAt, Instant expiresAt, String role) {
        var builder = JwtClaimsSet.builder().issuer(JwtService.ISSUER).subject("123")
                .issuedAt(issuedAt).claim("role", role);
        if (expiresAt != null) {
            builder.expiresAt(expiresAt);
        }
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), builder.build())).getTokenValue();
    }

    @Test
    void registerCreatesOnlyCustomerAndHashesPassword() throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.userId").value(321))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isSameAs(customerRole);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        var customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customers).saveAndFlush(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getUser()).isSameAs(saved);
        assertThat(customerCaptor.getValue().getCustomerName()).isEqualTo("Khách hàng");
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"role", "roleId", "status", "isAdmin"})
    void publicRegistrationRejectsPrivilegeFields(String field) throws Exception {
        String payload = registerJson().replace("}", ",\"" + field + "\":\"ADMIN\"}");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        when(users.existsByUsername("newcustomer")).thenReturn(true);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(users.existsByEmail("new@example.com")).thenReturn(true);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void blankOptionalEmailIsStoredAsNull() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson().replace("new@example.com", "")))
                .andExpect(status().isCreated());
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isNull();
        verify(users, never()).existsByEmail(any());
    }

    @Test
    void requestValidationRejectsBadEmailAndShortPassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson().replace("new@example.com", "invalid-email")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson().replace(PASSWORD, "short")))
                .andExpect(status().isBadRequest());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void unicodePasswordCannotExceedBcryptByteLimit() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson().replace(PASSWORD, "é".repeat(37))))
                .andExpect(status().isBadRequest());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void loginAuthenticatesWithBcryptAndReturnsMinimalJwt() throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"customer01\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600)).andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        LoginResponse response = mapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        Jwt jwt = decoder.decode(response.token());
        assertThat(jwt.getSubject()).isEqualTo("123");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(jwt.getClaims()).containsOnlyKeys("iss", "sub", "iat", "exp", "role");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer01", "missing"})
    void wrongPasswordAndMissingUsernameReturnSame401(String username) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không chính xác."))
                .andExpect(header().doesNotExist("Location"));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"INACTIVE", "LOCKED"})
    void disabledAccountsCannotLogin(UserStatus status) throws Exception {
        account.setStatus(status);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"customer01\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không chính xác."));
    }

    @Test
    void meWithValidTokenReturnsDtoWithoutSessionOrPassword() throws Exception {
        var result = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(123))
                .andExpect(jsonPath("$.username").value("customer01"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        verify(users, times(1)).findById(123L);
    }

    @Test
    void meWithoutTokenReturnsJson401WithoutRedirectOrSession() throws Exception {
        var result = mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(header().doesNotExist("Location")).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        verify(users, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-jwt", "aaa.bbb.ccc"})
    void malformedTokenReturns401(String value) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + value))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        verify(users, never()).findById(any());
    }

    @Test
    void tamperedSignatureReturns401() throws Exception {
        String[] parts = token().split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? "B" : "A") + parts[2].substring(1);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
        verify(users, never()).findById(any());
    }

    @Test
    void expiredTokenReturns401BeforeDatabaseLookup() throws Exception {
        String expired = signedToken(Instant.now().minusSeconds(3600), Instant.now().minusSeconds(120), "CUSTOMER");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        verify(users, never()).findById(any());
    }

    @Test
    void tokenWithoutExpirationIsRejected() throws Exception {
        String noExpiry = signedToken(Instant.now().minusSeconds(5), null, "CUSTOMER");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + noExpiry))
                .andExpect(status().isUnauthorized());
        verify(users, never()).findById(any());
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = {"INACTIVE", "LOCKED"})
    void tokenStopsWorkingAfterAccountIsDisabled(UserStatus status) throws Exception {
        String value = token();
        account.setStatus(status);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + value))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedUserCannotUsePreviouslyIssuedToken() throws Exception {
        String value = token();
        when(users.findById(123L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + value))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenCannotClaimDifferentRoleFromDatabase() throws Exception {
        String forgedRole = signedToken(Instant.now().minusSeconds(5), Instant.now().plusSeconds(3600), "ADMIN");
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + forgedRole))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbiddenFromAdminAndStaffEndpoints() throws Exception {
        String value = token();
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/staff/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/customer/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isOk());
    }

    @Test
    void staffCanUseStaffEndpointsButNotAdminOrCustomer() throws Exception {
        customerRole.setRoleName("STAFF");
        String value = token();
        mvc.perform(get("/api/staff/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/customer/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminAndStaffEndpoints() throws Exception {
        customerRole.setRoleName("ADMIN");
        String value = token();
        mvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isOk());
        mvc.perform(get("/api/staff/probe").header("Authorization", "Bearer " + value))
                .andExpect(status().isOk());
    }

    @Test
    void apiDoesNotAcceptWebSessionAuthentication() throws Exception {
        customerRole.setRoleName("ADMIN");
        var principal = new StoreUserDetails(account);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void webAnonymousRequestRedirectsToDefaultLoginPage() throws Exception {
        mvc.perform(get("/admin/probe")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void webFormLoginUsesDatabaseAndPersistsSession() throws Exception {
        customerRole.setRoleName("ADMIN");
        var result = mvc.perform(formLogin().user("customer01").password(PASSWORD))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/admin/probe").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());
    }

    @Test
    void webLoginPostWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/login").param("username", "customer01").param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void previouslyIssuedTokenIsRejectedAfterRoleChanges() throws Exception {
        String value = token();
        customerRole.setRoleName("STAFF");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + value))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingCustomerRolePreventsRegistrationBeforeAnyUserInsert() throws Exception {
        when(roles.findByRoleName("CUSTOMER")).thenReturn(Optional.empty());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson()))
                .andExpect(status().isInternalServerError());
        verify(users, never()).saveAndFlush(any());
        verify(customers, never()).saveAndFlush(any());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, AuthenticationConfig.class, JwtConfig.class,
            StoreUserDetailsService.class, JwtService.class, DatabaseJwtAuthenticationConverter.class,
            ApiSecurityErrorHandler.class, AuthService.class, AuthController.class, GlobalExceptionHandler.class,
            ProbeController.class, com.lauriel.store.controller.web.LoginController.class})
    static class TestConfig {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean RoleRepository roles() { return mock(RoleRepository.class); }
        @Bean CustomerRepository customers() { return mock(CustomerRepository.class); }
        @Bean ObjectMapper mapper() {
            return JsonMapper.builder().findAndAddModules()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        }
    }

    // Routes exist only in the test context, never in the production application.
    @RestController
    static class ProbeController {
        @GetMapping({"/api/admin/probe", "/api/staff/probe", "/api/customer/probe", "/admin/probe"})
        Map<String, Boolean> probe() { return Map.of("ok", true); }
    }
}
