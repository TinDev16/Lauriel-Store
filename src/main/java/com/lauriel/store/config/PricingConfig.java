package com.lauriel.store.config;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class PricingConfig {

    @Bean
    public Clock pricingClock() {
        return Clock.systemDefaultZone();
    }
}
