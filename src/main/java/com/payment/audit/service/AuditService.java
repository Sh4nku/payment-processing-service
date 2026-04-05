package com.payment.audit.service;

import com.payment.audit.entity.AuditLog;
import com.payment.audit.repository.AuditRepository;
import com.payment.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;

    @Transactional
    public void logEvent(TransactionEvent event) {
        AuditLog auditLog = AuditLog.builder()
                .transactionId(event.getTransactionId())
                .idempotencyKey(event.getIdempotencyKey())
                .sourceAccountNumber(event.getSourceAccountNumber())
                .destinationAccountNumber(event.getDestinationAccountNumber())
                .amount(event.getAmount())
                .transactionType(event.getType())
                .transactionStatus(event.getStatus())
                .description(event.getDescription())
                .build();

        auditRepository.save(auditLog);
        log.info("Audit log saved for transaction: {}", event.getTransactionId());
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditsByAccount(String accountNumber) {
        return auditRepository.findBySourceAccountNumberOrderByAuditedAtDesc(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditsByTransaction(UUID transactionId) {
        return auditRepository.findByTransactionIdOrderByAuditedAtDesc(transactionId);
    }
}
