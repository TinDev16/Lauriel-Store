package com.lauriel.store.event;

import static org.mockito.Mockito.*;

import com.lauriel.store.config.TelegramProperties;
import com.lauriel.store.service.TelegramNotificationService;
import com.lauriel.store.support.TestSchema;
import com.lauriel.store.support.TestSecrets;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrderCreatedTelegramListenerTest {
    @Autowired private ApplicationEventPublisher events;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private TelegramNotificationService telegram;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) throws Exception {
        String url = TestSchema.createIsolatedDatabase();
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.sql.init.mode", () -> "never");
        properties.add("app.jwt.secret", TestSecrets::randomBase64);
        properties.add("app.roles.initialize", () -> "true");
        properties.add("telegram.enabled", () -> "false");
    }

    @Test
    void committedTransactionInvokesAfterCommitListener() {
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> events.publishEvent(event(1L)));

        verify(telegram).sendNewOrderNotification(event(1L));
    }

    @Test
    void rolledBackTransactionNeverInvokesAfterCommitListener() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            events.publishEvent(event(2L));
            status.setRollbackOnly();
        });

        verifyNoInteractions(telegram);
    }

    private OrderCreatedEvent event(Long id) {
        return new OrderCreatedEvent(id, "Khách", "0901", "Địa chỉ",
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("11"),
                com.lauriel.store.entity.enums.OrderStatus.PENDING, null, List.of());
    }
}
