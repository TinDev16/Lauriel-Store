package com.lauriel.store.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private boolean enabled;
    private String botToken = "";
    private String chatId = "";
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(3);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken == null ? "" : botToken.strip(); }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId == null ? "" : chatId.strip(); }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = positive(connectTimeout, "telegram.connect-timeout");
    }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = positive(readTimeout, "telegram.read-timeout");
    }

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(botToken) && StringUtils.hasText(chatId);
    }

    private Duration positive(Duration value, String property) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(property + " phải lớn hơn 0");
        }
        return value;
    }
}
