package com.xanwar.rps.controller;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.xanwar.rps.dto.AuthRequest;
import com.xanwar.rps.dto.UserDto;
import com.xanwar.rps.model.User;
import com.xanwar.rps.repository.UserRepository;
import com.xanwar.rps.service.UserService;
import com.xanwar.rps.service.HouseAccountService;
import com.xanwar.rps.web.SessionKeys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xanwar.rps.util.ApiResponse;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final String REMEMBER_COOKIE = "rps_remember";
    private static final int REMEMBER_MAX_AGE = 60 * 60 * 24 * 30; // 30 days
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserService userService;
    private final UserRepository userRepository;
    private final HouseAccountService houseAccountService;

    public AuthController(UserService userService, UserRepository userRepository, HouseAccountService houseAccountService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.houseAccountService = houseAccountService;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @Valid @RequestBody AuthRequest request,
            HttpSession session,
            HttpServletResponse response
    ) {
        UserDto user = userService.signup(request.apiKey(), request.pin());
        session.setAttribute(SessionKeys.TORN_ID, user.tornId());
        issueRememberCookie(user.tornId(), response);

        Map<String, Object> body = ApiResponse.success("message", "Signup successful.");
        body.put("user", UserResponse.from(user, user.tornId().equals(houseAccountService.getRecipientId())));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody AuthRequest request,
            HttpSession session,
            HttpServletResponse response
    ) {
        UserDto user = userService.login(request.apiKey(), request.pin());
        session.setAttribute(SessionKeys.TORN_ID, user.tornId());
        issueRememberCookie(user.tornId(), response);

        Map<String, Object> body = ApiResponse.success("message", "Login successful.");
        body.put("user", UserResponse.from(user, user.tornId().equals(houseAccountService.getRecipientId())));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> authenticateCompat(
            @Valid @RequestBody AuthRequest request,
            HttpSession session,
            HttpServletResponse response
    ) {
        return login(request, session, response);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session, HttpServletRequest request) {
        Object tornId = session.getAttribute(SessionKeys.TORN_ID);
        if (tornId instanceof String id && !id.isBlank()) {
            return userService.findByTornId(id)
                    .map(user -> {
                        Map<String, Object> body = ApiResponse.success();
                        body.put("user", UserResponse.from(user, user.tornId().equals(houseAccountService.getRecipientId())));
                        return ResponseEntity.ok(body);
                    })
                    .orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found.")));
        }

        // Session expired — try remember-me cookie
        String token = getRememberCookieValue(request);
        if (token != null) {
            Optional<User> found = userRepository.findByRememberToken(token);
            if (found.isPresent()) {
                User user = found.get();
                session.setAttribute(SessionKeys.TORN_ID, user.getTornId());
                UserDto dto = userService.toDto(user);
                Map<String, Object> body = ApiResponse.success();
                body.put("user", UserResponse.from(dto, user.getTornId().equals(houseAccountService.getRecipientId())));
                return ResponseEntity.ok(body);
            }
        }

        return ResponseEntity.status(401).body(ApiResponse.error("Not logged in."));
    }

    @DeleteMapping("/auth")
    public ResponseEntity<Void> logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        // Clear remember token from DB
        String token = getRememberCookieValue(request);
        if (token != null) {
            userRepository.findByRememberToken(token).ifPresent(user -> {
                user.setRememberToken(null);
                userRepository.save(user);
            });
        }
        // Clear cookie
        Cookie cookie = new Cookie(REMEMBER_COOKIE, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);

        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserResponse>> getLeaderboard() {
        List<UserResponse> list = userService.getTopWinners().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/user/{tornId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String tornId) {
        if ("BOT_BAINING".equals(tornId)) {
            Map<String, Object> body = new HashMap<>();
            body.put("torn_id", "BOT_BAINING");
            body.put("username", "The House");
            body.put("site_balance", 9999999L);
            body.put("profile_image_url", "/baining.jpg");
            body.put("total_moola_betted", 1000000L);
            body.put("total_moola_won", 1250000L);
            body.put("total_moola_lost", 800000L);
            body.put("total_matches_played", 10000);
            body.put("total_matches_won", 5200);
            body.put("win_rate", 52.0);
            body.put("net_profit_loss", 450000L);
            return ResponseEntity.ok(body);
        }
        return userService.findByTornId(tornId)
                .map(user -> ResponseEntity.ok(UserResponse.toProfileMap(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Remember-me helpers ──────────────────────────────────────

    private void issueRememberCookie(String tornId, HttpServletResponse response) {
        byte[] raw = new byte[32];
        SECURE_RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        userRepository.findByTornId(tornId).ifPresent(user -> {
            user.setRememberToken(token);
            userRepository.save(user);
        });

        Cookie cookie = new Cookie(REMEMBER_COOKIE, token);
        cookie.setMaxAge(REMEMBER_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    private String getRememberCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (REMEMBER_COOKIE.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record UserResponse(
            String tornId,
            String username,
            long siteBalance,
            String profileImageUrl,
            long totalMoolaBetted,
            long totalMoolaWon,
            long totalMoolaLost,
            int totalMatchesPlayed,
            int totalMatchesWon,
            double winRate,
            long netProfitLoss,
            boolean isAdmin
    ) {
        static Map<String, Object> toProfileMap(UserDto dto) {
            Map<String, Object> body = new HashMap<>();
            body.put("torn_id", dto.tornId());
            body.put("username", dto.username());
            body.put("site_balance", dto.siteBalance());
            body.put("profile_image_url", dto.profileImageUrl());
            body.put("total_moola_betted", dto.totalMoolaBetted());
            body.put("total_moola_won", dto.totalMoolaWon());
            body.put("total_moola_lost", dto.totalMoolaLost());
            body.put("total_matches_played", dto.totalMatchesPlayed());
            body.put("total_matches_won", dto.totalMatchesWon());
            body.put("win_rate", dto.winRate());
            body.put("net_profit_loss", dto.netProfitLoss());
            return body;
        }

        static UserResponse from(UserDto dto, boolean isAdmin) {
            return new UserResponse(
                    dto.tornId(),
                    dto.username(),
                    dto.siteBalance(),
                    dto.profileImageUrl(),
                    dto.totalMoolaBetted(),
                    dto.totalMoolaWon(),
                    dto.totalMoolaLost(),
                    dto.totalMatchesPlayed(),
                    dto.totalMatchesWon(),
                    dto.winRate(),
                    dto.netProfitLoss(),
                    isAdmin
            );
        }

        static UserResponse from(UserDto dto) {
            return from(dto, false);
        }
    }
}
