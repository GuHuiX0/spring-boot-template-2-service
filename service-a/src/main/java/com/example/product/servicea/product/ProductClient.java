package com.example.product.servicea.product;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "service-b", url = "${service-b.base-url}")
public interface ProductClient {

    @PostMapping("/internal/products")
    ProductResponse create(@RequestBody ProductRequest request);

    @GetMapping("/internal/products")
    List<ProductResponse> findAll();

    @GetMapping("/internal/products/{id}")
    ProductResponse findById(@PathVariable long id);

    @PutMapping("/internal/products/{id}")
    ProductResponse update(@PathVariable long id, @RequestBody ProductRequest request);

    @DeleteMapping("/internal/products/{id}")
    void delete(@PathVariable long id);
}
