package com.xanwar.rps.service;

import com.xanwar.rps.config.AdminProperties;
import com.xanwar.rps.config.GameProperties;
import com.xanwar.rps.dto.WithdrawalDto;
import com.xanwar.rps.model.User;
import com.xanwar.rps.model.Withdrawal;
import com.xanwar.rps.repository.WithdrawalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WithdrawalService {

    private final UserService userService;
    private final WithdrawalRepository withdrawalRepository;
    private final GameProperties gameProperties;
    private final AdminProperties adminProperties;

    public WithdrawalService(
            UserService userService,
            WithdrawalRepository withdrawalRepository,
            GameProperties gameProperties,
            AdminProperties adminProperties
    ) {
        this.userService = userService;
        this.withdrawalRepository = withdrawalRepository;
        this.gameProperties = gameProperties;
        this.adminProperties = adminProperties;
    }

    @Transactional
    public Map<String, Object> requestWithdrawal(String tornId, long moolaAmount) {
        Map<String, Object> result = new HashMap<>();
        int step = gameProperties.getMoolaPerXanax();

        if (moolaAmount <= 0 || moolaAmount % step != 0) {
            result.put("success", false);
            result.put("error", "Amount must be a positive multiple of " + step + ".");
            return result;
        }

        User user = userService.requireUser(tornId);
        if (user.getSiteBalance() < moolaAmount) {
            result.put("success", false);
            result.put("error", "Insufficient balance.");
            return result;
        }

        user.setSiteBalance(user.getSiteBalance() - moolaAmount);
        int xanaxAmount = (int) (moolaAmount / step);
        Withdrawal withdrawal = new Withdrawal(tornId, user.getUsername(), moolaAmount, xanaxAmount);
        withdrawal.setUser(user);
        withdrawal.setStatus(Withdrawal.STATUS_PENDING);
        withdrawalRepository.save(withdrawal);

        result.put("success", true);
        result.put("message", "Withdrawal request submitted: " + moolaAmount
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
        Map<String, Object> result = new HashMap<>();
        assertAdmin(adminKey);

        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId).orElse(null);
        if (withdrawal == null) {
            result.put("success", false);
            result.put("error", "Withdrawal not found.");
            return result;
        }
        if (!Withdrawal.STATUS_PENDING.equals(withdrawal.getStatus())) {
            result.put("success", false);
            result.put("error", "Withdrawal already processed.");
            return result;
        }

        withdrawal.setStatus(Withdrawal.STATUS_COMPLETED);
        withdrawal.setCompletedAt(Instant.now());
        withdrawalRepository.save(withdrawal);

        result.put("success", true);
        result.put("message", "Withdrawal #" + withdrawalId + " marked as completed.");
        result.put("withdrawal", WithdrawalDto.from(withdrawal));
        return result;
    }

    private void assertAdmin(String adminKey) {
        if (!adminProperties.matchesKey(adminKey)) {
            throw new SecurityException("Unauthorized.");
        }
    }
}
