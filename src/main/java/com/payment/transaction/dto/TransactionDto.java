package com.payment.transaction.dto;

import com.payment.transaction.entity.TransactionStatus;
import com.payment.transaction.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferRequest {
        @NotBlank(message = "Source account is required")
        private String sourceAccountNumber;

        @NotBlank(message = "Destination account is required")
        private String destinationAccountNumber;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        private String description;

        @NotBlank(message = "Idempotency key is required")
        private String idempotencyKey;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID id;
        private String idempotencyKey;
        private String sourceAccountNumber;
        private String destinationAccountNumber;
        private BigDecimal amount;
        private TransactionType type;
        private TransactionStatus status;
        private String description;
        private String failureReason;
        private LocalDateTime createdAt;
    }
}
