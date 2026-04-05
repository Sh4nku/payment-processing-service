package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableJpaAuditing
public class PaymentProcessingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessingServiceApplication.class, args);
    }
}
