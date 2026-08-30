package com.example.product.servicea.error;

public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(Throwable cause) {
        super(cause);
    }
}
