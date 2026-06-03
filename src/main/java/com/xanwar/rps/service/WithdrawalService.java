package com.xanwar.rps.service;

import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.dto.WithdrawalDto;
import com.xanwar.rps.model.User;
import com.xanwar.rps.model.Withdrawal;
import com.xanwar.rps.repository.WithdrawalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xanwar.rps.util.ApiResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class WithdrawalService {

    private final UserService userService;
    private final WithdrawalRepository withdrawalRepository;
    private final GameProperties gameProperties;
    private final AdminAuthorizationService adminAuth;

    public WithdrawalService(
            UserService userService,
            WithdrawalRepository withdrawalRepository,
            GameProperties gameProperties,
            AdminAuthorizationService adminAuth
    ) {
        this.userService = userService;
        this.withdrawalRepository = withdrawalRepository;
        this.gameProperties = gameProperties;
        this.adminAuth = adminAuth;
    }

    @Transactional
    public Map<String, Object> requestWithdrawal(String tornId, long moolaAmount) {
        int step = gameProperties.getMoolaPerXanax();

        if (moolaAmount <= 0 || moolaAmount % step != 0) {
            return ApiResponse.error("Amount must be a positive multiple of " + step + ".");
        }

        User user = userService.requireUser(tornId);
        if (user.getSiteBalance() < moolaAmount) {
            return ApiResponse.error("Insufficient balance.");
        }

        user.setSiteBalance(user.getSiteBalance() - moolaAmount);
        userService.save(user);
        int xanaxAmount = (int) (moolaAmount / step);
        Withdrawal withdrawal = new Withdrawal(tornId, user.getUsername(), moolaAmount, xanaxAmount);
        withdrawal.setUser(user);
        withdrawal.setStatus(Withdrawal.STATUS_PENDING);
        withdrawalRepository.save(withdrawal);

        Map<String, Object> result = ApiResponse.success(
                "message", "Withdrawal request submitted: " + moolaAmount
                        + " Moola (" + xanaxAmount + " Xanax). Pending admin payout.");
        result.put("site_balance", user.getSiteBalance());
        result.put("withdrawal_id", withdrawal.getId());
        return result;
    }

    @Transactional(readOnly = true)
    public List<WithdrawalDto> listPending(String adminKey) {
        assertAdmin(adminKey);
        return withdrawalRepository.findByStatusOrderByCreatedAtAsc(Withdrawal.STATUS_PENDING)
                .stream()
                .map(WithdrawalDto::from)
                .toList();
    }

    @Transactional
    public Map<String, Object> complete(Long withdrawalId, String adminKey) {
        assertAdmin(adminKey);

        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId).orElse(null);
        if (withdrawal == null) {
            return ApiResponse.error("Withdrawal not found.");
        }
        if (!Withdrawal.STATUS_PENDING.equals(withdrawal.getStatus())) {
            return ApiResponse.error("Withdrawal already processed.");
        }

        withdrawal.setStatus(Withdrawal.STATUS_COMPLETED);
        withdrawal.setCompletedAt(Instant.now());
        withdrawalRepository.save(withdrawal);

        Map<String, Object> result = ApiResponse.success(
                "message", "Withdrawal #" + withdrawalId + " marked as completed.");
        result.put("withdrawal", WithdrawalDto.from(withdrawal));
        return result;
    }

    private void assertAdmin(String adminKey) {
        if (!adminAuth.isAuthorized(adminKey)) {
            throw new SecurityException("Unauthorized.");
        }
    }
}
