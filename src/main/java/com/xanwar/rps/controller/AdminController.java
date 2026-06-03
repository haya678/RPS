package com.xanwar.rps.controller;

import com.xanwar.rps.service.AdminAuthorizationService;
import com.xanwar.rps.dto.AdminKeyRequest;
import com.xanwar.rps.dto.WithdrawalDto;
import com.xanwar.rps.repository.UserRepository;
import com.xanwar.rps.service.WalletService;
import com.xanwar.rps.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final WithdrawalService withdrawalService;
    private final WalletService walletService;
    private final AdminAuthorizationService adminAuth;
    private final UserRepository userRepository;

    public AdminController(
            WithdrawalService withdrawalService,
            WalletService walletService,
            AdminAuthorizationService adminAuth,
            UserRepository userRepository
    ) {
        this.withdrawalService = withdrawalService;
        this.walletService = walletService;
        this.adminAuth = adminAuth;
        this.userRepository = userRepository;
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@Valid @RequestBody AdminKeyRequest request) {
        if (!adminAuth.isAuthorized(request.adminKey())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized."));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<Map<String, Object>> pending(@RequestHeader("X-Admin-Key") String adminKey) {
        List<WithdrawalDto> withdrawals = withdrawalService.listPending(adminKey);
        Map<String, Object> body = new HashMap<>();
        body.put("withdrawals", withdrawals);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/withdrawals/{id}/complete")
    public ResponseEntity<Map<String, Object>> complete(
            @PathVariable Long id,
            @Valid @RequestBody AdminKeyRequest request
    ) {
        return ResponseEntity.ok(withdrawalService.complete(id, request.adminKey()));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(@RequestHeader("X-Admin-Key") String adminKey) {
        if (!adminAuth.isAuthorized(adminKey)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized."));
        }

        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", u.getId());
                    map.put("username", u.getUsername());
                    map.put("tornId", u.getTornId());
                    map.put("siteBalance", u.getSiteBalance());
                    map.put("totalMatchesPlayed", u.getTotalMatchesPlayed());
                    map.put("totalMatchesWon", u.getTotalMatchesWon());
                    return map;
                })
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("users", users);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/credit")
    public ResponseEntity<Map<String, Object>> credit(
            @Valid @RequestBody CreditRequest request
    ) {
        if (!adminAuth.isAuthorized(request.adminKey())) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized."));
        }

        if (request.amount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Amount must be positive."));
        }

        boolean success = walletService.creditBalanceByUsername(request.username(), request.amount());
        if (!success) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User '" + request.username() + "' not found."));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully credited " + request.amount() + " Moola to user '" + request.username() + "'."
        ));
    }

    record CreditRequest(
            String username,
            long amount,
            String adminKey
    ) {}
}
