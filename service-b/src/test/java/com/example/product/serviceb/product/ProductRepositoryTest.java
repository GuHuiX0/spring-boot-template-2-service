package com.example.product.serviceb.product;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAndReloadsProductAfterFlywayMigration() {
        ProductEntity product = new ProductEntity(
                "Keyboard",
                "Mechanical keyboard",
                new BigDecimal("49.90"),
                8
        );

        ProductEntity saved = productRepository.saveAndFlush(product);
        entityManager.clear();

        ProductEntity reloaded = productRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getId()).isInstanceOf(Integer.class).isPositive();
        assertThat(reloaded.getName()).isEqualTo("Keyboard");
        assertThat(reloaded.getDescription()).isEqualTo("Mechanical keyboard");
        assertThat(reloaded.getPrice()).isEqualByComparingTo("49.90");
        assertThat(reloaded.getStockQuantity()).isEqualTo(8);
        assertThat(reloaded.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(reloaded.getUpdatedAt()).isEqualTo(saved.getUpdatedAt());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1",
                Integer.class
        )).isEqualTo(1);
    }
}
