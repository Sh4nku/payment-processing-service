package com.payment.transaction.repository;

import com.payment.transaction.entity.Transaction;
import com.payment.transaction.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findBySourceAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<Transaction> findByDestinationAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<Transaction> findByStatus(TransactionStatus status);
}
