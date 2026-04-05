package com.payment.account.service;

import com.payment.account.dto.AccountDto;
import com.payment.account.entity.Account;
import com.payment.account.entity.AccountStatus;
import com.payment.account.repository.AccountRepository;
import com.payment.exception.ErrorCode;
import com.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountDto.Response createAccount(AccountDto.CreateRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new PaymentException(ErrorCode.ACCOUNT_ALREADY_EXISTS,
                    "Account with email already exists: " + request.getEmail(),
                    HttpStatus.CONFLICT);
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .ownerName(request.getOwnerName())
                .email(request.getEmail())
                .balance(request.getInitialBalance())
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created: {}", saved.getAccountNumber());
        return mapToResponse(saved);
    }

    @Cacheable(value = "accounts", key = "#accountNumber")
    @Transactional(readOnly = true)
    public AccountDto.Response getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new PaymentException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found: " + accountNumber,
                        HttpStatus.NOT_FOUND));
        return mapToResponse(account);
    }

    @Cacheable(value = "balances", key = "#accountNumber")
    @Transactional(readOnly = true)
    public AccountDto.BalanceResponse getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new PaymentException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found: " + accountNumber,
                        HttpStatus.NOT_FOUND));
        return AccountDto.BalanceResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .asOf(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"accounts", "balances"}, key = "#accountNumber")
    @Transactional
    public AccountDto.Response updateAccountStatus(String accountNumber, AccountStatus status) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new PaymentException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Account not found: " + accountNumber,
                        HttpStatus.NOT_FOUND));
        account.setStatus(status);
        Account updated = accountRepository.save(account);
        log.info("Account {} status updated to {}", accountNumber, status);
        return mapToResponse(updated);
    }

    private String generateAccountNumber() {
        return "PAY" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private AccountDto.Response mapToResponse(Account account) {
        return AccountDto.Response.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .email(account.getEmail())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
