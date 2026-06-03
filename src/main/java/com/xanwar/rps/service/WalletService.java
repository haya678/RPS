package com.xanwar.rps.service;

import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

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
        userRepository.findByTornId(tornId).ifPresent(user -> {
            user.setSiteBalance(user.getSiteBalance() + amount);
        });
    }

    @Transactional
    public void refundBalance(String tornId, long amount) {
        creditBalance(tornId, amount);
    }

    @Transactional(readOnly = true)
    public long getBalance(String tornId) {
        return userRepository.findByTornId(tornId)
                .map(User::getSiteBalance)
                .orElse(0L);
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
}
