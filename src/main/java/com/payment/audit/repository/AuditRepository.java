package com.payment.audit.repository;

import com.payment.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findBySourceAccountNumberOrderByAuditedAtDesc(String accountNumber);

    List<AuditLog> findByTransactionIdOrderByAuditedAtDesc(UUID transactionId);
}
