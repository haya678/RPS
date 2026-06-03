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

import com.xanwar.rps.util.ApiResponse;

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
        ResponseEntity<Map<String, Object>> denied = requireAdmin(request.adminKey());
        if (denied != null) return denied;
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<Map<String, Object>> pending(@RequestParam String adminKey) {
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
    public ResponseEntity<Map<String, Object>> listUsers(@RequestParam String adminKey) {
        ResponseEntity<Map<String, Object>> denied = requireAdmin(adminKey);
        if (denied != null) return denied;

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
        ResponseEntity<Map<String, Object>> denied = requireAdmin(request.adminKey());
        if (denied != null) return denied;

        if (request.amount() <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Amount must be positive."));
        }

        boolean success = walletService.creditBalanceByUsername(request.username(), request.amount());
        if (!success) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User '" + request.username() + "' not found."));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "message", "Successfully credited " + request.amount() + " Moola to user '" + request.username() + "'."
        ));
    }

    private ResponseEntity<Map<String, Object>> requireAdmin(String adminKey) {
        if (!adminAuth.isAuthorized(adminKey)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized."));
        }
        return null;
    }

    record CreditRequest(
            String username,
            long amount,
            String adminKey
    ) {}
}
