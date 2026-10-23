package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.lauriel.store.config.TelegramProperties;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.event.OrderCreatedEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TelegramNotificationServiceTest {

    @Test
    void disabledTelegramDoesNotCallHttp() {
        var properties = configured();
        properties.setEnabled(false);
        var builder = RestClient.builder().baseUrl("https://api.telegram.org");
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = service(properties, builder);

        service.sendNewOrderNotification(event());

        server.verify();
    }

    @Test
    void sendsExpectedPayloadWithoutExposingTokenInMessage() {
        var properties = configured();
        var builder = RestClient.builder().baseUrl("https://api.telegram.org");
        var server = MockRestServiceServer.bindTo(builder).build();
        server
            .expect(
                requestTo("https://api.telegram.org/botfake-token/sendMessage")
            )
            .andExpect(
                content().string(containsString("\"chat_id\":\"12345\""))
            )
            .andExpect(content().string(containsString("Mã đơn: #125")))
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.not(containsString("fake-token"))
                )
            )
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service(properties, builder).sendNewOrderNotification(event());

        server.verify();
    }

    @Test
    void telegramHttpFailureNeverEscapesToOrderFlow() {
        var properties = configured();
        var builder = RestClient.builder().baseUrl("https://api.telegram.org");
        var server = MockRestServiceServer.bindTo(builder).build();
        server
            .expect(
                requestTo("https://api.telegram.org/botfake-token/sendMessage")
            )
            .andRespond(withServerError());

        assertThatCode(() ->
            service(properties, builder).sendNewOrderNotification(event())
        ).doesNotThrowAnyException();

        server.verify();
    }

    private TelegramNotificationService service(
        TelegramProperties properties,
        RestClient.Builder builder
    ) {
        return new TelegramNotificationService(
            properties,
            new TelegramOrderMessageFormatter(),
            builder.build(),
            new SyncTaskExecutor()
        );
    }

    private TelegramProperties configured() {
        var properties = new TelegramProperties();
        properties.setEnabled(true);
        properties.setBotToken("fake-token");
        properties.setChatId("12345");
        return properties;
    }

    private OrderCreatedEvent event() {
        return new OrderCreatedEvent(
            125L,
            "Customer",
            "0901",
            "Address",
            new BigDecimal("100000"),
            BigDecimal.ZERO,
            new BigDecimal("20000"),
            new BigDecimal("120000"),
            OrderStatus.PENDING,
            LocalDateTime.of(2026, 9, 30, 20, 15),
            List.of(new OrderCreatedEvent.Item("Áo", "M", "Đen", 1))
        );
    }
}
