package com.xanwar.rps.service;

import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final UserRepository userRepository;

    public WalletService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean deductBalance(String tornId, long amount) {
        User user = userRepository.findByTornId(tornId).orElse(null);
        if (user == null || user.getSiteBalance() < amount) {
            return false;
        }
        user.setSiteBalance(user.getSiteBalance() - amount);
        return true;
    }

    @Transactional
    public void creditBalance(String tornId, long amount) {
        User user = userRepository.findByTornId(tornId).orElse(null);
        if (user == null) {
            log.error("creditBalance failed: no user found for tornId={}, amount={}", tornId, amount);
            throw new IllegalArgumentException("Cannot credit balance: user not found for tornId=" + tornId);
        }
        user.setSiteBalance(user.getSiteBalance() + amount);
    }

    @Transactional
    public void refundBalance(String tornId, long amount) {
        User user = userRepository.findByTornId(tornId).orElse(null);
        if (user == null) {
            log.error("refundBalance failed: no user found for tornId={}, amount={}", tornId, amount);
            throw new IllegalArgumentException("Cannot refund balance: user not found for tornId=" + tornId);
        }
        user.setSiteBalance(user.getSiteBalance() + amount);
        log.info("Refunded {} to tornId={}", amount, tornId);
    }

    @Transactional(readOnly = true)
    public long getBalance(String tornId) {
        return userRepository.findByTornId(tornId)
                .map(User::getSiteBalance)
                .orElseThrow(() -> {
                    log.error("getBalance failed: no user found for tornId={}", tornId);
                    return new IllegalArgumentException("User not found for tornId=" + tornId);
                });
    }

    @Transactional
    public boolean creditBalanceByUsername(String username, long amount) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return false;
        }
        user.setSiteBalance(user.safeBalance() + amount);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public void tip(String fromTornId, String toTornId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Tip amount must be positive");
        }
        if (fromTornId.equals(toTornId)) {
            throw new IllegalArgumentException("You cannot tip yourself");
        }
        
        User fromUser = userRepository.findByTornId(fromTornId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User toUser = userRepository.findByTornId(toTornId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        if (fromUser.getSiteBalance() < amount) {
            throw new IllegalStateException("Insufficient balance to tip " + amount + " Moola");
        }

        fromUser.setSiteBalance(fromUser.getSiteBalance() - amount);
        toUser.setSiteBalance(toUser.getSiteBalance() + amount);
        
        log.info("Player {} tipped {} Moola to {}", fromTornId, amount, toTornId);
    }
}
