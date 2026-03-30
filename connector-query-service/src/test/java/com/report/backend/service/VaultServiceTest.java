package com.report.backend.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VaultServiceTest {

    private VaultService vaultService;
    private String testFilePath;

    @BeforeEach
    void setUp() throws Exception {
        testFilePath = "data/test-vault-" + UUID.randomUUID() + ".json";
        
        vaultService = new VaultService("test-secret-key-12345", testFilePath);
        ReflectionTestUtils.invokeMethod(vaultService, "init");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get(testFilePath));
    }

    @Test
    void testEncryptDecryptFlow() {
        String connectorName = "test_connector";
        String plaintext = "superSecretPassword!";

        // Store password (encrypts and saves)
        vaultService.storePassword(connectorName, plaintext);
        
        // Retrieve and decrypt
        String decrypted = vaultService.getPassword(connectorName);
        assertEquals(plaintext, decrypted);
    }
    
    @Test
    void testDeletePassword() {
        String connectorName = "delete_target";
        vaultService.storePassword(connectorName, "temp123");
        assertNotNull(vaultService.getPassword(connectorName));
        
        vaultService.deletePassword(connectorName);
        assertNull(vaultService.getPassword(connectorName));
    }

    @Test
    void testPasswordPersistence() throws Exception {
        String connectorName = "persist_test";
        String plaintext = "persistMe123";
        vaultService.storePassword(connectorName, plaintext);

        // Simulate service restart by creating a new instance
        VaultService newVaultInstance = new VaultService("test-secret-key-12345", testFilePath);
        ReflectionTestUtils.invokeMethod(newVaultInstance, "init");

        String retrieved = newVaultInstance.getPassword(connectorName);
        assertEquals(plaintext, retrieved);
    }
}
