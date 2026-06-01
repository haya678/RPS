package com.xanwar.rps.controller;

import com.xanwar.rps.dto.WithdrawRequest;
import com.xanwar.rps.service.WithdrawalService;
import com.xanwar.rps.web.SessionUserResolver;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final SessionUserResolver sessionUserResolver;

    public WithdrawalController(WithdrawalService withdrawalService, SessionUserResolver sessionUserResolver) {
        this.withdrawalService = withdrawalService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            HttpSession session
    ) {
        String tornId = sessionUserResolver.resolveTornId(session, request.tornId());
        Map<String, Object> result = withdrawalService.requestWithdrawal(tornId, request.moolaAmount());
        return ResponseEntity.ok(result);
    }
}
