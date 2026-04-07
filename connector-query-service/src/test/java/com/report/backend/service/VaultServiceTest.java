package com.report.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VaultServiceTest {

    private VaultService vaultService;
    private File tempVault;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        tempVault = tempDir.resolve("vault.json").toFile();
        vaultService = new VaultService("1234567890123456", tempVault.getAbsolutePath());
    }

    @Test
    void testEncryptDecrypt() {
        String plain = "password123";
        String encrypted = vaultService.encrypt(plain);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);

        String decrypted = vaultService.decrypt(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    void testStoreAndGetPassword() {
        vaultService.storePassword("test-connector", "secret-pass");
        String result = vaultService.getPassword("test-connector");
        assertEquals("secret-pass", result);
    }

    @Test
    void testGetPassword_NotFound_ThrowsException() {
        assertThrows(RuntimeException.class, () -> vaultService.getPassword("non-existent"));
    }

    @Test
    void testDeletePassword() {
        vaultService.storePassword("to-delete", "pass");
        vaultService.deletePassword("to-delete");
        assertThrows(RuntimeException.class, () -> vaultService.getPassword("to-delete"));
    }

    @Test
    void testEncryptDecrypt_Null_ReturnsNull() {
        assertNull(vaultService.encrypt(null));
        assertNull(vaultService.decrypt(null));
    }

    @Test
    void testDecrypt_InvalidBase64_ThrowsException() {
        assertThrows(RuntimeException.class, () -> vaultService.decrypt("not-base64!"));
    }
}
