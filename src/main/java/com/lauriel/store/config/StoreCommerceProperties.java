package com.lauriel.store.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "store")
public class StoreCommerceProperties {
    private BigDecimal shippingFee = new BigDecimal("20000.00");

    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) {
        if (shippingFee == null || shippingFee.signum() < 0) {
            throw new IllegalArgumentException("store.shipping-fee phải là số không âm");
        }
        this.shippingFee = shippingFee;
    }
}
