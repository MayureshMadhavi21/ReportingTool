package com.report.backend.controller;

import com.report.backend.service.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @GetMapping("/encrypt")
    public ResponseEntity<Map<String, String>> encrypt(@RequestParam String text) {
        return ResponseEntity.ok(Map.of("encrypted", vaultService.encrypt(text)));
    }

    @GetMapping("/decrypt")
    public ResponseEntity<Map<String, String>> decrypt(@RequestParam String text) {
        return ResponseEntity.ok(Map.of("decrypted", vaultService.decrypt(text)));
    }
}
