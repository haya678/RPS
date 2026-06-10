package com.xanwar.rps.controller;

import com.xanwar.rps.util.ApiResponse;
import com.xanwar.rps.dto.TipRequest;
import com.xanwar.rps.service.WalletService;
import com.xanwar.rps.web.SessionUserResolver;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final WalletService walletService;
    private final SessionUserResolver sessionUserResolver;

    public UserController(WalletService walletService, SessionUserResolver sessionUserResolver) {
        this.walletService = walletService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @PostMapping("/tip")
    public ResponseEntity<Map<String, Object>> tipPlayer(@RequestBody TipRequest request, HttpSession session) {
        try {
            String sessionTornId = sessionUserResolver.resolveTornId(session, request.fromTornId());
            if (!sessionTornId.equals(request.fromTornId())) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Session mismatch"));
            }

            walletService.tip(request.fromTornId(), request.toTornId(), request.amount());
            return ResponseEntity.ok(ApiResponse.success("message", "Successfully tipped " + request.amount() + " Moola!"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("An unexpected error occurred during tipping"));
        }
    }
}
