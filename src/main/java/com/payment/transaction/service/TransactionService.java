package com.payment.transaction.service;

import com.payment.account.entity.Account;
import com.payment.account.entity.AccountStatus;
import com.payment.account.repository.AccountRepository;
import com.payment.exception.ErrorCode;
import com.payment.exception.PaymentException;
import com.payment.kafka.event.TransactionEvent;
import com.payment.kafka.producer.TransactionEventProducer;
import com.payment.transaction.dto.TransactionDto;
import com.payment.transaction.entity.Transaction;
import com.payment.transaction.entity.TransactionStatus;
import com.payment.transaction.entity.TransactionType;
import com.payment.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionEventProducer eventProducer;

    @Transactional
    @CircuitBreaker(name = "paymentService", fallbackMethod = "transferFallback")
    @Retry(name = "paymentService")
    public TransactionDto.Response transfer(TransactionDto.TransferRequest request) {

        // Idempotency check
        transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw new PaymentException(ErrorCode.DUPLICATE_TRANSACTION,
                            "Duplicate transaction with idempotency key: " + request.getIdempotencyKey(),
                            HttpStatus.CONFLICT);
                });

        // Validate amount
        if (request.getAmount().signum() <= 0) {
            throw new PaymentException(ErrorCode.INVALID_AMOUNT,
                    "Transfer amount must be greater than zero",
                    HttpStatus.BAD_REQUEST);
        }

        // Fetch accounts with pessimistic locking via @Version (optimistic)
        Account source = accountRepository.findActiveByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new PaymentException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Source account not found or inactive: " + request.getSourceAccountNumber(),
                        HttpStatus.NOT_FOUND));

        Account destination = accountRepository.findActiveByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new PaymentException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Destination account not found or inactive: " + request.getDestinationAccountNumber(),
                        HttpStatus.NOT_FOUND));

        // Check sufficient balance
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new PaymentException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Insufficient balance in account: " + request.getSourceAccountNumber(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Create pending transaction record
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .sourceAccountNumber(request.getSourceAccountNumber())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        transactionRepository.save(transaction);

        try {
            // Debit source
            source.setBalance(source.getBalance().subtract(request.getAmount()));
            accountRepository.save(source);

            // Credit destination
            destination.setBalance(destination.getBalance().add(request.getAmount()));
            accountRepository.save(destination);

            // Mark success
            transaction.setStatus(TransactionStatus.SUCCESS);
            Transaction saved = transactionRepository.save(transaction);

            log.info("Transfer successful: {} -> {} amount: {}",
                    request.getSourceAccountNumber(),
                    request.getDestinationAccountNumber(),
                    request.getAmount());

            // Publish Kafka event
            publishEvent(saved);

            return mapToResponse(saved);

        } catch (Exception ex) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(ex.getMessage());
            transactionRepository.save(transaction);
            log.error("Transfer failed: {}", ex.getMessage());
            throw ex;
        }
    }

    public TransactionDto.Response transferFallback(TransactionDto.TransferRequest request, Throwable t) {
        log.error("Circuit breaker triggered for transfer: {}", t.getMessage());
        throw new PaymentException(ErrorCode.INVALID_REQUEST,
                "Payment service is temporarily unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Transactional(readOnly = true)
    public TransactionDto.Response getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "Transaction not found: " + transactionId,
                        HttpStatus.NOT_FOUND));
        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findBySourceAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void publishEvent(Transaction transaction) {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .sourceAccountNumber(transaction.getSourceAccountNumber())
                .destinationAccountNumber(transaction.getDestinationAccountNumber())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .timestamp(LocalDateTime.now())
                .build();
        eventProducer.publishTransactionEvent(event);
    }

    private TransactionDto.Response mapToResponse(Transaction transaction) {
        return TransactionDto.Response.builder()
                .id(transaction.getId())
                .idempotencyKey(transaction.getIdempotencyKey())
                .sourceAccountNumber(transaction.getSourceAccountNumber())
                .destinationAccountNumber(transaction.getDestinationAccountNumber())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
