package com.report.backend.controller;

import com.report.backend.service.VaultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VaultController.class)
class VaultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VaultService vaultService;

    @Test
    void encrypt_ShouldReturnEncryptedText() throws Exception {
        when(vaultService.encrypt("plain")).thenReturn("encrypted-stuff");

        mockMvc.perform(get("/api/vault/encrypt").param("text", "plain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encrypted").value("encrypted-stuff"));

        verify(vaultService).encrypt("plain");
    }

    @Test
    void decrypt_ShouldReturnDecryptedText() throws Exception {
        when(vaultService.decrypt("encrypted")).thenReturn("plain-text");

        mockMvc.perform(get("/api/vault/decrypt").param("text", "encrypted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decrypted").value("plain-text"));

        verify(vaultService).decrypt("encrypted");
    }
}
