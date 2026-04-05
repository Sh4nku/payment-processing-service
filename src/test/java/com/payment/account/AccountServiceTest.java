package com.payment.account;

import com.payment.account.dto.AccountDto;
import com.payment.account.entity.Account;
import com.payment.account.entity.AccountStatus;
import com.payment.account.repository.AccountRepository;
import com.payment.account.service.AccountService;
import com.payment.exception.PaymentException;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockAccount = Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("PAY123456")
                .ownerName("Ashish Singh")
                .email("ashish@test.com")
                .balance(new BigDecimal("10000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void createAccount_Success() {
        AccountDto.CreateRequest request = AccountDto.CreateRequest.builder()
                .ownerName("Ashish Singh")
                .email("ashish@test.com")
                .initialBalance(new BigDecimal("10000.00"))
                .build();

        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        AccountDto.Response response = accountService.createAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.getOwnerName()).isEqualTo("Ashish Singh");
        assertThat(response.getBalance()).isEqualByComparingTo("10000.00");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void createAccount_DuplicateEmail_ThrowsException() {
        AccountDto.CreateRequest request = AccountDto.CreateRequest.builder()
                .ownerName("Ashish Singh")
                .email("ashish@test.com")
                .initialBalance(new BigDecimal("10000.00"))
                .build();

        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Account with email already exists");
    }

    @Test
    void getAccount_NotFound_ThrowsException() {
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount("INVALID"))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void getBalance_Success() {
        when(accountRepository.findByAccountNumber("PAY123456")).thenReturn(Optional.of(mockAccount));

        AccountDto.BalanceResponse response = accountService.getBalance("PAY123456");

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo("10000.00");
        assertThat(response.getAccountNumber()).isEqualTo("PAY123456");
    }
}
