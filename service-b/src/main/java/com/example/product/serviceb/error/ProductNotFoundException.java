package com.example.product.serviceb.error;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Product " + id + " was not found");
    }
}
