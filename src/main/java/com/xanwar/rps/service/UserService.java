package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.dto.UserDto;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TornApiClient tornApiClient;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TornApiClient tornApiClient, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tornApiClient = tornApiClient;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getTopWinners() {
        return userRepository.findTop10ByOrderByTotalMatchesWonDesc().stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public UserDto signup(String playerApiKey, String pin) {
        validatePin(pin);
        TornIdentity identity = fetchIdentity(playerApiKey);
        Optional<User> existing = userRepository.findByTornId(identity.tornId());
        if (existing.isPresent() && existing.get().getPinHash() != null && !existing.get().getPinHash().isBlank()) {
            throw new IllegalArgumentException("Account already exists. Please log in with your PIN.");
        }
        User user = existing.orElseGet(() -> new User(identity.tornId(), identity.username()));
        user.setUsername(identity.username());
        user.setProfileImageUrl(identity.profileImageUrl());
        user.setPinHash(passwordEncoder.encode(pin));
        user.setLastLogin(Instant.now());
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto login(String playerApiKey, String pin) {
        validatePin(pin);
        TornIdentity identity = fetchIdentity(playerApiKey);
        User user = userRepository.findByTornId(identity.tornId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found. Please sign up first."));

        if (user.getPinHash() == null || user.getPinHash().isBlank()) {
            throw new IllegalArgumentException("PIN is not set for this account. Please contact support.");
        }
        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            throw new IllegalArgumentException("Incorrect 4-digit PIN.");
        }

        user.setUsername(identity.username());
        user.setProfileImageUrl(identity.profileImageUrl());
        user.setLastLogin(Instant.now());
        return UserDto.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByTornId(String tornId) {
        return userRepository.findByTornId(tornId).map(UserDto::from);
    }

    @Transactional(readOnly = true)
    public User requireUser(String tornId) {
        return userRepository.findByTornId(tornId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional
    public void recordBet(String tornId, long amount) {
        User user = requireUser(tornId);
        long current = user.getTotalMoolaBetted() == null ? 0L : user.getTotalMoolaBetted();
        user.setTotalMoolaBetted(current + Math.max(0, amount));
    }

    @Transactional
    public void recordMatchOutcome(String winnerId, String loserId, long winnerPayout, long betAmount) {
        if (!"BOT_BAINING".equals(winnerId)) {
            User winner = requireUser(winnerId);
            winner.setTotalMatchesPlayed((winner.getTotalMatchesPlayed() == null ? 0 : winner.getTotalMatchesPlayed()) + 1);
            winner.setTotalMatchesWon((winner.getTotalMatchesWon() == null ? 0 : winner.getTotalMatchesWon()) + 1);
            long netProfit = Math.max(0L, winnerPayout - betAmount);
            long currentWon = winner.getTotalMoolaWon() == null ? 0L : winner.getTotalMoolaWon();
            winner.setTotalMoolaWon(currentWon + netProfit);
            userRepository.save(winner);
        }
        if (!"BOT_BAINING".equals(loserId)) {
            User loser = requireUser(loserId);
            loser.setTotalMatchesPlayed((loser.getTotalMatchesPlayed() == null ? 0 : loser.getTotalMatchesPlayed()) + 1);
            long stake = Math.max(0L, betAmount);
            long currentLost = loser.getTotalMoolaLost() == null ? 0L : loser.getTotalMoolaLost();
            loser.setTotalMoolaLost(currentLost + stake);
            userRepository.save(loser);
        }
    }

    private void validatePin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
    }

    private TornIdentity fetchIdentity(String playerApiKey) {
        JsonNode data = tornApiClient.fetchUserProfile(playerApiKey);
        String tornId = String.valueOf(data.path("player_id").asInt());
        String username = data.path("name").asText();
        String profileImageUrl = resolveProfileImage(data, tornId);
        return new TornIdentity(tornId, username, profileImageUrl);
    }

    private String resolveProfileImage(JsonNode data, String tornId) {
        if (data.hasNonNull("profile_image")) {
            String value = data.path("profile_image").asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        JsonNode icons = data.path("icons");
        if (icons.isObject() && icons.hasNonNull("icon75")) {
            String value = icons.path("icon75").asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "https://images.torn.com/avatars/" + tornId + ".png";
    }

    private record TornIdentity(String tornId, String username, String profileImageUrl) {}
}
