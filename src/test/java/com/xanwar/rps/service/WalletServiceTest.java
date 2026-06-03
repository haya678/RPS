package com.xanwar.rps.service;

import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("123", "TestUser");
        user.setSiteBalance(500L);
    }

    @Test
    void deductBalanceSucceedsWhenSufficientFunds() {
        when(userRepository.findByTornId("123")).thenReturn(Optional.of(user));

        boolean result = walletService.deductBalance("123", 200L);

        assertThat(result).isTrue();
        assertThat(user.getSiteBalance()).isEqualTo(300L);
    }

    @Test
    void deductBalanceFailsWhenInsufficientFunds() {
        when(userRepository.findByTornId("123")).thenReturn(Optional.of(user));

        boolean result = walletService.deductBalance("123", 600L);

        assertThat(result).isFalse();
        assertThat(user.getSiteBalance()).isEqualTo(500L);
    }

    @Test
    void deductBalanceFailsForUnknownUser() {
        when(userRepository.findByTornId("unknown")).thenReturn(Optional.empty());

        boolean result = walletService.deductBalance("unknown", 100L);

        assertThat(result).isFalse();
    }

    @Test
    void deductBalanceFailsWhenExactBalanceRequested() {
        when(userRepository.findByTornId("123")).thenReturn(Optional.of(user));

        boolean result = walletService.deductBalance("123", 500L);

        assertThat(result).isTrue();
        assertThat(user.getSiteBalance()).isEqualTo(0L);
    }

    @Test
    void creditBalanceAddsToExistingBalance() {
        when(userRepository.findByTornId("123")).thenReturn(Optional.of(user));

        walletService.creditBalance("123", 150L);

        assertThat(user.getSiteBalance()).isEqualTo(650L);
    }

    @Test
    void creditBalanceThrowsForUnknownUser() {
        when(userRepository.findByTornId("unknown")).thenReturn(Optional.empty());

        try {
            walletService.creditBalance("unknown", 100L);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("user not found");
        }

        verify(userRepository).findByTornId("unknown");
    }

    @Test
    void getBalanceReturnsUserBalance() {
        when(userRepository.findByTornId("123")).thenReturn(Optional.of(user));

        long balance = walletService.getBalance("123");

        assertThat(balance).isEqualTo(500L);
    }

    @Test
    void getBalanceThrowsForUnknownUser() {
        when(userRepository.findByTornId("unknown")).thenReturn(Optional.empty());

        try {
            walletService.getBalance("unknown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("User not found");
        }
    }

    @Test
    void creditBalanceByUsernameSucceeds() {
        when(userRepository.findByUsernameIgnoreCase("TestUser")).thenReturn(Optional.of(user));

        boolean result = walletService.creditBalanceByUsername("TestUser", 100L);

        assertThat(result).isTrue();
        assertThat(user.getSiteBalance()).isEqualTo(600L);
        verify(userRepository).save(user);
    }

    @Test
    void creditBalanceByUsernameFailsForUnknownUser() {
        when(userRepository.findByUsernameIgnoreCase("Nobody")).thenReturn(Optional.empty());

        boolean result = walletService.creditBalanceByUsername("Nobody", 100L);

        assertThat(result).isFalse();
    }

    @Test
    void creditBalanceByUsernameHandlesNullBalance() {
        user.setSiteBalance(null);
        when(userRepository.findByUsernameIgnoreCase("TestUser")).thenReturn(Optional.of(user));

        boolean result = walletService.creditBalanceByUsername("TestUser", 50L);

        assertThat(result).isTrue();
        assertThat(user.getSiteBalance()).isEqualTo(50L);
    }
}
