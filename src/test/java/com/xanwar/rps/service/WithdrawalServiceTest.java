package com.xanwar.rps.service;

import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.dto.WithdrawalDto;
import com.xanwar.rps.model.User;
import com.xanwar.rps.model.Withdrawal;
import com.xanwar.rps.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock private UserService userService;
    @Mock private WithdrawalRepository withdrawalRepository;
    @Mock private AdminAuthorizationService adminAuth;

    private GameProperties gameProperties;
    private WithdrawalService withdrawalService;

    @BeforeEach
    void setUp() {
        gameProperties = new GameProperties();
        gameProperties.setMoolaPerXanax(4);

        withdrawalService = new WithdrawalService(
                userService, withdrawalRepository, gameProperties, adminAuth
        );
    }

    @Test
    void requestWithdrawalSucceedsWithValidAmount() {
        User user = new User("123", "TestUser");
        user.setSiteBalance(100L);
        when(userService.requireUser("123")).thenReturn(user);

        Map<String, Object> result = withdrawalService.requestWithdrawal("123", 20L);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(user.getSiteBalance()).isEqualTo(80L);
        verify(withdrawalRepository).save(any(Withdrawal.class));
    }

    @Test
    void requestWithdrawalFailsWithInvalidAmount() {
        // Not a multiple of 4
        Map<String, Object> result = withdrawalService.requestWithdrawal("123", 15L);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("Amount must be a positive multiple of 4.");
    }

    @Test
    void requestWithdrawalFailsWithInsufficientBalance() {
        User user = new User("123", "TestUser");
        user.setSiteBalance(10L);
        when(userService.requireUser("123")).thenReturn(user);

        Map<String, Object> result = withdrawalService.requestWithdrawal("123", 20L);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("Insufficient balance.");
    }

    @Test
    void listPendingRequiresAdmin() {
        when(adminAuth.isAuthorized("wrong-key")).thenReturn(false);

        assertThatThrownBy(() -> withdrawalService.listPending("wrong-key"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void listPendingReturnsPendingWithdrawals() {
        when(adminAuth.isAuthorized("admin-key")).thenReturn(true);
        Withdrawal w = new Withdrawal("123", "User", 40L, 10);
        w.setStatus(Withdrawal.STATUS_PENDING);
        when(withdrawalRepository.findByStatusOrderByCreatedAtAsc(Withdrawal.STATUS_PENDING))
                .thenReturn(List.of(w));

        List<WithdrawalDto> result = withdrawalService.listPending("admin-key");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tornId()).isEqualTo("123");
    }

    @Test
    void completeSucceedsForPendingWithdrawal() {
        when(adminAuth.isAuthorized("admin-key")).thenReturn(true);
        Withdrawal w = new Withdrawal("123", "User", 40L, 10);
        w.setStatus(Withdrawal.STATUS_PENDING);
        when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(w));

        Map<String, Object> result = withdrawalService.complete(1L, "admin-key");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(w.getStatus()).isEqualTo(Withdrawal.STATUS_COMPLETED);
        assertThat(w.getCompletedAt()).isNotNull();
        verify(withdrawalRepository).save(w);
    }

    @Test
    void completeFailsForAlreadyProcessedWithdrawal() {
        when(adminAuth.isAuthorized("admin-key")).thenReturn(true);
        Withdrawal w = new Withdrawal("123", "User", 40L, 10);
        w.setStatus(Withdrawal.STATUS_COMPLETED);
        when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(w));

        Map<String, Object> result = withdrawalService.complete(1L, "admin-key");

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("Withdrawal already processed.");
    }
}
