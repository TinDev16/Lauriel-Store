package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lauriel.store.entity.Product;
import com.lauriel.store.exception.ResourceNotFoundException;
import com.lauriel.store.repository.ProductRepository;
import com.lauriel.store.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductServiceTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository, mock(PricingService.class));

    @Test
    void returnsProductDtoWithoutTraversingLazyVariants() {
        Product product = new Product();
        product.setProductId(1L);
        product.setProductName("Áo Lauriel");
        product.setPrice(new BigDecimal("250000.00"));
        product.setVariants(null);
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        var response = service.getById(1L);
        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.productName()).isEqualTo("Áo Lauriel");
        assertThat(response.price()).isEqualByComparingTo("250000.00");
    }

    @Test
    void rejectsMissingProduct() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
