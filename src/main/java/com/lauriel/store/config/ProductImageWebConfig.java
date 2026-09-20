package com.lauriel.store.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProductImageWebConfig implements WebMvcConfigurer {
    private final String location;

    public ProductImageWebConfig(@Value("${app.images.directory:uploads/products}") String location) {
        this.location = location;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uri = Paths.get(location).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(uri.endsWith("/") ? uri : uri + "/");
    }
}