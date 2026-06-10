package com.xanwar.rps.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xanwar.rps.client.TornApiClient;
import com.xanwar.rps.dto.MatchHistoryDto;
import com.xanwar.rps.dto.UserDto;
import com.xanwar.rps.model.MatchResult;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.MatchResultRepository;
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
    private final MatchResultRepository matchResultRepository;
    private final TornApiClient tornApiClient;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, MatchResultRepository matchResultRepository, TornApiClient tornApiClient, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.matchResultRepository = matchResultRepository;
        this.tornApiClient = tornApiClient;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getTopWinners() {
        return userRepository.findTopProfitableUsers(org.springframework.data.domain.PageRequest.of(0, 10)).stream()
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

    public UserDto toDto(User user) {
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public User requireUser(String tornId) {
        return userRepository.findByTornId(tornId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional
    public void recordBet(String tornId, long amount) {
        User user = requireUser(tornId);
        user.setTotalMoolaBetted(user.safeMoolaBetted() + Math.max(0, amount));
    }

    @Transactional
    public void recordMatchOutcome(String winnerId, String loserId, long winnerPayout, long betAmount, String p1Id, String p1Name, String p2Id, String p2Name, boolean isForfeit) {
        if (!"BOT_BAINING".equals(winnerId)) {
            User winner = requireUser(winnerId);
            winner.setTotalMatchesPlayed(winner.safeMatchesPlayed() + 1);
            winner.setTotalMatchesWon(winner.safeMatchesWon() + 1);
            long netProfit = Math.max(0L, winnerPayout - betAmount);
            winner.setTotalMoolaWon(winner.safeMoolaWon() + netProfit);
            userRepository.save(winner);
        }
        if (!"BOT_BAINING".equals(loserId)) {
            User loser = requireUser(loserId);
            loser.setTotalMatchesPlayed(loser.safeMatchesPlayed() + 1);
            long stake = Math.max(0L, betAmount);
            loser.setTotalMoolaLost(loser.safeMoolaLost() + stake);
            userRepository.save(loser);
        }

        // Save historical match result
        MatchResult result = new MatchResult(
            p1Id, p1Name, p2Id, p2Name, winnerId, betAmount * 2, betAmount, isForfeit
        );
        matchResultRepository.save(result);
    }

    @Transactional(readOnly = true)
    public List<MatchHistoryDto> getMatchHistory(String tornId) {
        return matchResultRepository.findByPlayer1IdOrPlayer2IdOrderByCreatedAtDesc(tornId, tornId).stream()
                .map(m -> {
                    boolean won = tornId.equals(m.getWinnerId());
                    String opponentId = tornId.equals(m.getPlayer1Id()) ? m.getPlayer2Id() : m.getPlayer1Id();
                    String opponentName = tornId.equals(m.getPlayer1Id()) ? m.getPlayer2Name() : m.getPlayer1Name();
                    String profilePic = resolveOpponentProfilePic(opponentId);
                    return new MatchHistoryDto(
                            won,
                            m.getPotAmount(),
                            opponentName,
                            profilePic,
                            m.getCreatedAt(),
                            m.isForfeit()
                    );
                })
                .toList();
    }

    private String resolveOpponentProfilePic(String opponentId) {
        if ("BOT_BAINING".equals(opponentId)) {
            return "/baining.jpg";
        }
        return userRepository.findByTornId(opponentId)
                .map(User::getProfileImageUrl)
                .orElse("https://images.torn.com/avatars/" + opponentId + ".png");
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
