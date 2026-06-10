package com.xanwar.rps.controller;

import com.xanwar.rps.util.ApiResponse;
import com.xanwar.rps.dto.TipRequest;
import com.xanwar.rps.service.WalletService;
import com.xanwar.rps.web.SessionUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse> tipPlayer(@RequestBody TipRequest request, HttpServletRequest servletRequest) {
        String sessionTornId = sessionUserResolver.resolveTornId(servletRequest);
        if (sessionTornId == null || !sessionTornId.equals(request.fromTornId())) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Session mismatch"));
        }

        try {
            walletService.tip(request.fromTornId(), request.toTornId(), request.amount());
            return ResponseEntity.ok(ApiResponse.success("Successfully tipped " + request.amount() + " Moola!"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("An unexpected error occurred during tipping"));
        }
    }
}
