package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Widget")
                .description("A widget")
                .price(BigDecimal.valueOf(19.99))
                .quantity(10)
                .category("gadgets")
                .sku("WIDGET-1")
                .build();
    }

    @Test
    void createProduct_success() {
        ProductRequest request = ProductRequest.builder()
                .name("Widget").description("A widget").price(BigDecimal.valueOf(19.99))
                .quantity(10).category("gadgets").sku("WIDGET-1").build();
        when(productRepository.existsBySku("WIDGET-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("WIDGET-1", response.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_throwsWhenSkuExists() {
        ProductRequest request = ProductRequest.builder()
                .name("Widget").price(BigDecimal.ONE).quantity(1).category("c").sku("DUP").build();
        when(productRepository.existsBySku("DUP")).thenReturn(true);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void checkStock_sufficientStock_returnsTrue() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertTrue(productService.checkStock(1L, 5));
    }

    @Test
    void checkStock_insufficientStock_returnsFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertFalse(productService.checkStock(1L, 50));
    }

    @Test
    void checkStock_productNotFound_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.checkStock(999L, 1));
    }

    @Test
    void reduceInventoryIfAvailable_delegatesToAtomicUpdate_andReturnsTrueOnSuccess() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.decrementStockIfAvailable(1L, 3)).thenReturn(1);

        assertTrue(productService.reduceInventoryIfAvailable(1L, 3));
        verify(productRepository).decrementStockIfAvailable(1L, 3);
    }

    @Test
    void reduceInventoryIfAvailable_returnsFalse_whenAtomicUpdateAffectsNoRows() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.decrementStockIfAvailable(1L, 999)).thenReturn(0);

        assertFalse(productService.reduceInventoryIfAvailable(1L, 999));
    }

    @Test
    void reduceInventoryIfAvailable_throwsWhenProductMissing() {
        when(productRepository.existsById(999L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> productService.reduceInventoryIfAvailable(999L, 1));
    }

    @Test
    void reduceInventory_throwsInsufficientStock_whenAtomicUpdateFails() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.decrementStockIfAvailable(1L, 999)).thenReturn(0);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> productService.reduceInventory(1L, 999));
    }

    @Test
    void restoreInventory_incrementsStock() {
        when(productRepository.incrementStock(1L, 4)).thenReturn(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.restoreInventory(1L, 4);

        assertNotNull(response);
        verify(productRepository).incrementStock(1L, 4);
    }

    @Test
    void restoreInventory_throwsWhenProductMissing() {
        when(productRepository.incrementStock(999L, 1)).thenReturn(0);

        assertThrows(ResourceNotFoundException.class, () -> productService.restoreInventory(999L, 1));
    }
}
