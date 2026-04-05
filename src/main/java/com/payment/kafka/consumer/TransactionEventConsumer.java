package com.payment.kafka.consumer;

import com.payment.audit.service.AuditService;
import com.payment.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final AuditService auditService;

    @KafkaListener(topics = "payment-transactions", groupId = "payment-group")
    public void consumeTransactionEvent(TransactionEvent event) {
        log.info("Received transaction event: {} status: {}", event.getTransactionId(), event.getStatus());
        auditService.logEvent(event);
    }
}
