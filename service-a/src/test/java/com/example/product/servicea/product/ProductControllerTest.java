package com.example.product.servicea.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String PRODUCT_JSON = """
            {"name":"Keyboard","description":"Mechanical keyboard","price":49.90,"stockQuantity":8}
            """;

    private static final ProductRequest PRODUCT_REQUEST = new ProductRequest(
            "Keyboard", "Mechanical keyboard", new BigDecimal("49.90"), 8
    );

    private static final ProductResponse PRODUCT_RESPONSE = new ProductResponse(
            1L,
            "Keyboard",
            "Mechanical keyboard",
            new BigDecimal("49.90"),
            8,
            Instant.parse("2024-01-01T10:15:30Z"),
            Instant.parse("2024-01-02T11:16:31Z")
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductClient productClient;

    @Test
    void createsProductAtThePublicLocationWithEveryResponseField() throws Exception {
        when(productClient.create(PRODUCT_REQUEST)).thenReturn(PRODUCT_RESPONSE);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.description").value("Mechanical keyboard"))
                .andExpect(jsonPath("$.price").value(49.90))
                .andExpect(jsonPath("$.stockQuantity").value(8))
                .andExpect(jsonPath("$.createdAt").value("2024-01-01T10:15:30Z"))
                .andExpect(jsonPath("$.updatedAt").value("2024-01-02T11:16:31Z"));

        verify(productClient).create(PRODUCT_REQUEST);
    }

    @Test
    void listsProductsInTheUpstreamOrder() throws Exception {
        ProductResponse mouse = new ProductResponse(
                2L,
                "Mouse",
                null,
                new BigDecimal("19.99"),
                4,
                Instant.parse("2024-01-03T12:17:32Z"),
                Instant.parse("2024-01-04T13:18:33Z")
        );
        when(productClient.findAll()).thenReturn(List.of(PRODUCT_RESPONSE, mouse));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Keyboard"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Mouse"));

        verify(productClient).findAll();
    }

    @Test
    void findsProductById() throws Exception {
        when(productClient.findById(1L)).thenReturn(PRODUCT_RESPONSE);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"));

        verify(productClient).findById(1L);
    }

    @Test
    void updatesProductWithTheRequestBodyAndId() throws Exception {
        when(productClient.update(1L, PRODUCT_REQUEST)).thenReturn(PRODUCT_RESPONSE);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"));

        verify(productClient).update(1L, PRODUCT_REQUEST);
    }

    @Test
    void deletesProductByIdWithoutAResponseBody() throws Exception {
        doNothing().when(productClient).delete(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(productClient).delete(1L);
    }

    @Test
    void rejectsBlankNameWithoutCallingServiceB() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","description":"Mechanical keyboard","price":49.90,"stockQuantity":8}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());

        verifyNoInteractions(productClient);
    }

    @Test
    void rejectsNegativePriceWithoutCallingServiceB() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Keyboard","description":"Mechanical keyboard","price":-0.01,"stockQuantity":8}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.price").exists());

        verifyNoInteractions(productClient);
    }

    @Test
    void rejectsNegativeStockWithoutCallingServiceB() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Keyboard","description":"Mechanical keyboard","price":49.90,"stockQuantity":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.stockQuantity").exists());

        verifyNoInteractions(productClient);
    }
}
