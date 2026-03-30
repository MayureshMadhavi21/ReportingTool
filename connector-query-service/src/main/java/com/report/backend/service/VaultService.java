package com.report.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class VaultService {

    private final String secretKey;
    private final File vaultFile;
    private final ObjectMapper objectMapper;
    private final Map<String, String> vaultCache;

    public VaultService(
            @Value("${vault.secret.key:1234567890123456}") String secretKey,
            @Value("${vault.file.path:data/vault.json}") String vaultFilePath) {
        // Ensure 16/24/32 bytes for AES
        this.secretKey = String.format("%-16s", secretKey).substring(0, 16);
        this.vaultFile = new File(vaultFilePath);
        this.objectMapper = new ObjectMapper();
        this.vaultCache = new ConcurrentHashMap<>();
        
        loadVault();
    }

    private void loadVault() {
        if (vaultFile.exists()) {
            try {
                Map<String, String> data = objectMapper.readValue(vaultFile, new TypeReference<>() {});
                vaultCache.putAll(data);
                log.info("Vault cache loaded successfully with {} entries.", vaultCache.size());
            } catch (IOException e) {
                log.error("Failed to load vault file", e);
            }
        } else {
            vaultFile.getParentFile().mkdirs();
            saveVault();
        }
    }

    private synchronized void saveVault() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(vaultFile, vaultCache);
        } catch (IOException e) {
            log.error("Failed to save vault file", e);
            throw new RuntimeException("Failed to save secure vault configuration", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public void storePassword(String connectorName, String plainPassword) {
        vaultCache.put(connectorName, encrypt(plainPassword));
        saveVault();
    }

    public String getPassword(String connectorName) {
        String encrypted = vaultCache.get(connectorName);
        if (encrypted == null) {
            throw new RuntimeException("Password not found in vault for: " + connectorName);
        }
        return decrypt(encrypted);
    }
    
    public void deletePassword(String connectorName) {
        vaultCache.remove(connectorName);
        saveVault();
    }
}
