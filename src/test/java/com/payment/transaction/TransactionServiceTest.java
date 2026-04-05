package com.payment.transaction;

import com.payment.account.entity.Account;
import com.payment.account.entity.AccountStatus;
import com.payment.account.repository.AccountRepository;
import com.payment.exception.PaymentException;
import com.payment.kafka.producer.TransactionEventProducer;
import com.payment.transaction.dto.TransactionDto;
import com.payment.transaction.entity.Transaction;
import com.payment.transaction.entity.TransactionStatus;
import com.payment.transaction.entity.TransactionType;
import com.payment.transaction.repository.TransactionRepository;
import com.payment.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionEventProducer eventProducer;

    @InjectMocks
    private TransactionService transactionService;

    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("PAY-SOURCE")
                .ownerName("Ashish Singh")
                .email("ashish@test.com")
                .balance(new BigDecimal("5000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        destinationAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("PAY-DEST")
                .ownerName("Rohit Sharma")
                .email("rohit@test.com")
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void transfer_Success() {
        TransactionDto.TransferRequest request = TransactionDto.TransferRequest.builder()
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("1000.00"))
                .idempotencyKey(UUID.randomUUID().toString())
                .description("Test transfer")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(request.getIdempotencyKey())
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("1000.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .build();

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountRepository.findActiveByAccountNumber("PAY-SOURCE")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findActiveByAccountNumber("PAY-DEST")).thenReturn(Optional.of(destinationAccount));
        when(transactionRepository.save(any())).thenReturn(savedTransaction);
        when(accountRepository.save(any())).thenReturn(sourceAccount);

        TransactionDto.Response response = transactionService.transfer(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(eventProducer, times(1)).publishTransactionEvent(any());
    }

    @Test
    void transfer_DuplicateIdempotencyKey_ThrowsException() {
        TransactionDto.TransferRequest request = TransactionDto.TransferRequest.builder()
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("500.00"))
                .idempotencyKey("EXISTING-KEY")
                .build();

        Transaction existingTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("EXISTING-KEY")
                .status(TransactionStatus.SUCCESS)
                .build();

        when(transactionRepository.findByIdempotencyKey("EXISTING-KEY"))
                .thenReturn(Optional.of(existingTransaction));

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Duplicate transaction");
    }
}
