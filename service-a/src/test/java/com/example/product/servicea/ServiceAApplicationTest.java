package com.example.product.servicea;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.cloud.compatibility-verifier.enabled=false")
class ServiceAApplicationTest {

    @Test
    void contextLoads() {
    }
}
