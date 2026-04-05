package com.payment.kafka.producer;

import com.payment.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private static final String TOPIC = "payment-transactions";
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void publishTransactionEvent(TransactionEvent event) {
        kafkaTemplate.send(TOPIC, event.getIdempotencyKey(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction event: {}", event.getTransactionId(), ex);
                    } else {
                        log.info("Transaction event published: {} to partition {}",
                                event.getTransactionId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
