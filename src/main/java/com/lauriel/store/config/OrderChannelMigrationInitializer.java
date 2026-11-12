package com.lauriel.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class OrderChannelMigrationInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
        OrderChannelMigrationInitializer.class
    );
    private final JdbcTemplate jdbcTemplate;

    public OrderChannelMigrationInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int updated = jdbcTemplate.update(
                "UPDATE orders SET sales_channel = 'MOBILE' WHERE sales_channel = 'UNKNOWN'"
            );
            if (updated > 0) {
                log.info(
                    "Đã chuyển {} đơn hàng từ UNKNOWN sang MOBILE sales_channel",
                    updated
                );
            }
        } catch (Exception ex) {
            log.warn(
                "Không thể di chuyển dữ liệu UNKNOWN sales_channel: {}",
                ex.getMessage()
            );
        }
    }
}

