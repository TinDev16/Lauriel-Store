package com.lauriel.store.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DualPortWebConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> storefrontConnector(
        @Value("${server.port:8080}") int adminPort,
        @Value("${app.storefront.port:8081}") int storefrontPort
    ) {
        return factory -> {
            if (storefrontPort == adminPort) {
                return;
            }
            Connector connector = new Connector();
            connector.setPort(storefrontPort);
            factory.addAdditionalConnectors(connector);
        };
    }
}
