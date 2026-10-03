package com.lauriel.store.service;

import com.lauriel.store.config.TelegramProperties;
import com.lauriel.store.event.OrderCreatedEvent;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class TelegramNotificationService {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private final TelegramProperties properties;
    private final TelegramOrderMessageFormatter formatter;
    private final RestClient client;
    private final TaskExecutor executor;

    public TelegramNotificationService(
            TelegramProperties properties,
            TelegramOrderMessageFormatter formatter,
            @Qualifier("telegramRestClient") RestClient client,
            @Qualifier("telegramTaskExecutor") TaskExecutor executor) {
        this.properties = properties;
        this.formatter = formatter;
        this.client = client;
        this.executor = executor;
    }

    public void sendNewOrderNotification(OrderCreatedEvent event) {
        if (!properties.isConfigured()) return;
        try {
            executor.execute(() -> deliver(event));
        } catch (RuntimeException exception) {
            log.warn("Không thể xếp thông báo Telegram cho đơn #{} ({})",
                    event.orderId(), exception.getClass().getSimpleName());
        }
    }

    private void deliver(OrderCreatedEvent event) {
        try {
            client.post()
                    .uri("/bot" + properties.getBotToken() + "/sendMessage")
                    .body(Map.of(
                            "chat_id", properties.getChatId(),
                            "text", formatter.format(event)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            log.warn("Gửi Telegram cho đơn #{} thất bại ({})",
                    event.orderId(), exception.getClass().getSimpleName());
        }
    }
}
