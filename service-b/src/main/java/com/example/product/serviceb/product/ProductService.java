package com.example.product.serviceb.product;

import com.example.product.serviceb.error.ProductNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        Instant now = Instant.now();
        ProductEntity product = new ProductEntity(
                request.name().trim(),
                normalizeDescription(request.description()),
                request.price(),
                request.stockQuantity()
        );
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        return toResponse(productRepository.save(product));
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse findById(long id) {
        return toResponse(requireProduct(id));
    }

    public ProductResponse update(long id, ProductRequest request) {
        ProductEntity product = requireProduct(id);
        product.setName(request.name().trim());
        product.setDescription(normalizeDescription(request.description()));
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setUpdatedAt(Instant.now());
        return toResponse(productRepository.save(product));
    }

    public void delete(long id) {
        productRepository.delete(requireProduct(id));
    }

    private ProductEntity requireProduct(long id) {
        return productRepository.findById(toRepositoryId(id))
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Integer toRepositoryId(long id) {
        if (id < 1 || id > Integer.MAX_VALUE) {
            throw new ProductNotFoundException(id);
        }
        return (int) id;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private ProductResponse toResponse(ProductEntity product) {
        return new ProductResponse(
                product.getId().longValue(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
