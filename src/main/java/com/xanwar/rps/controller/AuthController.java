package com.xanwar.rps.controller;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.xanwar.rps.dto.AuthRequest;
import com.xanwar.rps.dto.UserDto;
import com.xanwar.rps.service.UserService;
import com.xanwar.rps.service.HouseAccountService;
import com.xanwar.rps.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.xanwar.rps.util.ApiResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final HouseAccountService houseAccountService;

    public AuthController(UserService userService, HouseAccountService houseAccountService) {
        this.userService = userService;
        this.houseAccountService = houseAccountService;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @Valid @RequestBody AuthRequest request,
            HttpSession session
    ) {
        UserDto user = userService.signup(request.apiKey(), request.pin());
        session.setAttribute(SessionKeys.TORN_ID, user.tornId());

        Map<String, Object> body = ApiResponse.success("message", "Signup successful.");
        body.put("user", UserResponse.from(user, user.tornId().equals(houseAccountService.getRecipientId())));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody AuthRequest request,
            HttpSession session
    ) {
        UserDto user = userService.login(request.apiKey(), request.pin());
        session.setAttribute(SessionKeys.TORN_ID, user.tornId());

        Map<String, Object> body = ApiResponse.success("message", "Login successful.");
        body.put("user", UserResponse.from(user, user.tornId().equals(houseAccountService.getRecipientId())));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> authenticateCompat(
            @Valid @RequestBody AuthRequest request,
            HttpSession session
    ) {
        return login(request, session);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> me(HttpSession session) {
        Object tornId = session.getAttribute(SessionKeys.TORN_ID);
        if (!(tornId instanceof String id) || id.isBlank()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not logged in."));
        }
        return userService.findByTornId(id)
                .map(user -> {
                    Map<String, Object> body = ApiResponse.success();
                    body.put("user", UserResponse.from(user, user.tornId().equals(houseAccountService.getRecipientId())));
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found.")));
    }

    @DeleteMapping("/auth")
    public ResponseEntity<Void> logout(HttpSession session) {
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
