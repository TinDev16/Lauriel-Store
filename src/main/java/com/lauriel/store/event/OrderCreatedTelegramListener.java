package com.lauriel.store.event;

import com.lauriel.store.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedTelegramListener {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedTelegramListener.class);
    private final TelegramNotificationService telegram;

    public OrderCreatedTelegramListener(TelegramNotificationService telegram) {
        this.telegram = telegram;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            telegram.sendNewOrderNotification(event);
        } catch (RuntimeException exception) {
            log.warn("Không thể xếp thông báo Telegram cho đơn #{} ({})",
                    event.orderId(), exception.getClass().getSimpleName());
        }
    }
}
