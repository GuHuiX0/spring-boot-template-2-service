package com.example.product.serviceb.product;

import com.example.product.serviceb.error.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void createsTrimmedProductAndSavesMappedFields() {
        ProductRequest request = new ProductRequest(
                "  Keyboard  ",
                "  Mechanical keyboard  ",
                new BigDecimal("49.90"),
                8
        );
        when(productRepository.save(org.mockito.ArgumentMatchers.any(ProductEntity.class)))
                .thenAnswer(invocation -> {
                    ProductEntity product = invocation.getArgument(0);
                    product.setId(7L);
                    return product;
                });
        Instant beforeCreate = Instant.now();

        ProductResponse response = productService.create(request);
        Instant afterCreate = Instant.now();

        ArgumentCaptor<ProductEntity> productCaptor = ArgumentCaptor.forClass(ProductEntity.class);
        verify(productRepository).save(productCaptor.capture());
        ProductEntity saved = productCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Keyboard");
        assertThat(saved.getDescription()).isEqualTo("Mechanical keyboard");
        assertThat(saved.getPrice()).isEqualByComparingTo("49.90");
        assertThat(saved.getStockQuantity()).isEqualTo(8);
        assertThat(saved.getCreatedAt()).isBetween(beforeCreate, afterCreate);
        assertThat(saved.getUpdatedAt()).isBetween(beforeCreate, afterCreate);
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.createdAt()).isEqualTo(saved.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void returnsProductsInAscendingRepositoryOrder() {
        ProductEntity first = product(1L, "Keyboard", "Mechanical keyboard", "49.90", 8);
        ProductEntity second = product(2L, "Mouse", null, "19.99", 4);
        when(productRepository.findAllByOrderByIdAsc()).thenReturn(List.of(first, second));

        List<ProductResponse> products = productService.findAll();

        assertThat(products).extracting(ProductResponse::id).containsExactly(1L, 2L);
        assertThat(products).extracting(ProductResponse::name).containsExactly("Keyboard", "Mouse");
        verify(productRepository).findAllByOrderByIdAsc();
    }

    @Test
    void findsProductById() {
        ProductEntity product = product(42L, "Keyboard", "Mechanical keyboard", "49.90", 8);
        when(productRepository.findById(42L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(42L);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.description()).isEqualTo("Mechanical keyboard");
        assertThat(response.price()).isEqualByComparingTo("49.90");
        assertThat(response.stockQuantity()).isEqualTo(8);
    }

    @Test
    void updatesProductWhilePreservingCreatedAtAndAdvancingUpdatedAt() {
        ProductEntity existing = product(42L, "Keyboard", "Mechanical keyboard", "49.90", 8);
        Instant createdAt = existing.getCreatedAt();
        when(productRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        ProductResponse response = productService.update(42L, new ProductRequest(
                "  Updated keyboard  ",
                "   ",
                new BigDecimal("59.90"),
                6
        ));

        assertThat(existing.getName()).isEqualTo("Updated keyboard");
        assertThat(existing.getDescription()).isNull();
        assertThat(existing.getPrice()).isEqualByComparingTo("59.90");
        assertThat(existing.getStockQuantity()).isEqualTo(6);
        assertThat(existing.getCreatedAt()).isEqualTo(createdAt);
        assertThat(existing.getUpdatedAt()).isAfter(createdAt);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(existing.getUpdatedAt());
        verify(productRepository).save(existing);
    }

    @Test
    void deletesExistingProduct() {
        ProductEntity product = product(42L, "Keyboard", "Mechanical keyboard", "49.90", 8);
        when(productRepository.findById(42L)).thenReturn(Optional.of(product));

        productService.delete(42L);

        verify(productRepository).delete(product);
    }

    @Test
    void throwsWhenRequestedProductDoesNotExist() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(42L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product 42 was not found");
    }

    @Test
    void throwsWhenUpdatedProductDoesNotExist() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(42L, new ProductRequest(
                "Keyboard", "Mechanical keyboard", new BigDecimal("49.90"), 8
        )))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product 42 was not found");
    }

    @Test
    void throwsWhenDeletedProductDoesNotExist() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(42L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product 42 was not found");
    }

    private ProductEntity product(Long id, String name, String description, String price, int stockQuantity) {
        ProductEntity product = new ProductEntity(name, description, new BigDecimal(price), stockQuantity);
        product.setId(id);
        product.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        product.setUpdatedAt(Instant.parse("2024-01-01T00:00:01Z"));
        return product;
    }
}
