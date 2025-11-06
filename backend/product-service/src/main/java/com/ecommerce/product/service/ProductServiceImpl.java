package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ProductService
 * Handles all business logic for product management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSku());

        // Check if SKU already exists
        if (productRepository.existsBySku(request.getSku())) {
            log.error("Product with SKU {} already exists", request.getSku());
            throw new DataIntegrityViolationException("Product with SKU " + request.getSku() + " already exists");
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .sku(request.getSku())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Successfully created product with ID: {} and SKU: {}", savedProduct.getId(), savedProduct.getSku());

        return ProductResponse.fromEntity(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with ID: " + id);
                });

        // Check if SKU is being changed and if new SKU already exists
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            log.error("Product with SKU {} already exists", request.getSku());
            throw new DataIntegrityViolationException("Product with SKU " + request.getSku() + " already exists");
        }

        // Update fields
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setSku(request.getSku());

        Product updatedProduct = productRepository.save(product);
        log.info("Successfully updated product with ID: {}", id);

        return ProductResponse.fromEntity(updatedProduct);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with ID: " + id);
                });

        return ProductResponse.fromEntity(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        log.debug("Fetching all products");

        List<Product> products = productRepository.findAll();
        log.info("Found {} products", products.size());

        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategory(String category) {
        log.debug("Fetching products by category: {}", category);

        List<Product> products = productRepository.findByCategory(category);
        log.info("Found {} products in category: {}", products.size(), category);

        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchProductsByName(String searchTerm) {
        log.debug("Searching products with term: {}", searchTerm);

        List<Product> products = productRepository.findByNameContaining(searchTerm);
        log.info("Found {} products matching search term: {}", products.size(), searchTerm);

        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);

        if (!productRepository.existsById(id)) {
            log.error("Product not found with ID: {}", id);
            throw new ResourceNotFoundException("Product not found with ID: " + id);
        }

        productRepository.deleteById(id);
        log.info("Successfully deleted product with ID: {}", id);
    }

    @Override
    @Transactional
    public ProductResponse updateInventory(Long id, Integer quantity) {
        log.info("Updating inventory for product ID: {} to quantity: {}", id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with ID: " + id);
                });

        product.setQuantity(quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Successfully updated inventory for product ID: {}", id);

        return ProductResponse.fromEntity(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse reduceInventory(Long id, Integer amount) {
        log.info("Reducing inventory for product ID: {} by amount: {}", id, amount);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with ID: " + id);
                });

        if (!product.hasSufficientStock(amount)) {
            log.error("Insufficient stock for product ID: {}. Required: {}, Available: {}",
                    id, amount, product.getQuantity());
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName() +
                    ". Required: " + amount + ", Available: " + product.getQuantity());
        }

        product.reduceQuantity(amount);
        Product updatedProduct = productRepository.save(product);
        log.info("Successfully reduced inventory for product ID: {}. New quantity: {}",
                id, updatedProduct.getQuantity());

        return ProductResponse.fromEntity(updatedProduct);
    }

    @Override
    public ProductResponse getProductBySku(String sku) {
        log.debug("Fetching product with SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> {
                    log.error("Product not found with SKU: {}", sku);
                    return new ResourceNotFoundException("Product not found with SKU: " + sku);
                });

        return ProductResponse.fromEntity(product);
    }

    @Override
    public boolean checkStock(Long id, Integer requiredQuantity) {
        log.debug("Checking stock for product ID: {} with required quantity: {}", id, requiredQuantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ResourceNotFoundException("Product not found with ID: " + id);
                });

        boolean hasStock = product.hasSufficientStock(requiredQuantity);
        log.debug("Stock check result for product ID {}: {}", id, hasStock);

        return hasStock;
    }

    @Override
    public List<ProductResponse> getLowStockProducts(Integer threshold) {
        log.debug("Fetching low stock products with threshold: {}", threshold);

        List<Product> products = productRepository.findLowStockProducts(threshold);
        log.info("Found {} low stock products", products.size());

        return products.stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
