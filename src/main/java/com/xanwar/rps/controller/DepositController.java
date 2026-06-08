package com.xanwar.rps.controller;

import com.xanwar.rps.dto.DepositVerifyRequest;
import com.xanwar.rps.service.DepositService;
import com.xanwar.rps.service.HouseAccountService;
import com.xanwar.rps.web.SessionUserResolver;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/deposit")
public class DepositController {

    private final DepositService depositService;
    private final HouseAccountService houseAccountService;
    private final SessionUserResolver sessionUserResolver;

    public DepositController(
            DepositService depositService,
            HouseAccountService houseAccountService,
            SessionUserResolver sessionUserResolver
    ) {
        this.depositService = depositService;
        this.houseAccountService = houseAccountService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @GetMapping("/instructions")
    public ResponseEntity<Map<String, Object>> instructions() {
        return ResponseEntity.ok(houseAccountService.depositInstructions());
    }

    /** Legacy path: POST /api/deposit (frontend). */
    @PostMapping({"", "/verify"})
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody(required = false) DepositVerifyRequest request,
            HttpSession session
    ) {
        String tornId = sessionUserResolver.resolveTornId(
                session,
                request != null ? request.tornId() : null
        );
        return ResponseEntity.ok(depositService.verifyDeposit(tornId));
    }

    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiate(
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        String tornId = sessionUserResolver.resolveTornId(session, (String) body.get("tornId"));
        int xanaxAmount = Integer.parseInt(String.valueOf(body.get("xanaxAmount")));
        return ResponseEntity.ok(depositService.initiateDeposit(tornId, xanaxAmount));
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> checkStatus(
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        String tornId = sessionUserResolver.resolveTornId(session, (String) body.get("tornId"));
        return ResponseEntity.ok(depositService.checkStatus(tornId));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        String tornId = sessionUserResolver.resolveTornId(session, (String) body.get("tornId"));
        return ResponseEntity.ok(depositService.cancelDeposit(tornId));
    }
}
