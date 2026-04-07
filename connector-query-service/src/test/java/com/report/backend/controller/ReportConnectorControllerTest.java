package com.report.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.service.ReportConnectorService;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportConnectorController.class)
class ReportConnectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportConnectorService connectorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllConnectors_ShouldReturnList() throws Exception {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        when(connectorService.getAllConnectors()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/connectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(dto.getId()))
                .andExpect(jsonPath("$[0].name").value(dto.getName()));
    }

    @Test
    void getConnectorById_Found_ShouldReturnDto() throws Exception {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        when(connectorService.getConnectorById("conn-123")).thenReturn(dto);

        mockMvc.perform(get("/api/connectors/conn-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dto.getId()));
    }

    @Test
    void createConnector_ValidInput_ShouldReturnCreated() throws Exception {
        ReportConnectorDto inputDto = TestDataFactory.createConnectorDto();
        inputDto.setId(null);
        ReportConnectorDto savedDto = TestDataFactory.createConnectorDto();

        when(connectorService.createConnector(any(ReportConnectorDto.class))).thenReturn(savedDto);

        mockMvc.perform(post("/api/connectors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedDto.getId()));

        verify(connectorService).createConnector(any(ReportConnectorDto.class));
    }

    @Test
    void updateConnector_ValidInput_ShouldReturnUpdated() throws Exception {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        when(connectorService.updateConnector(eq("conn-123"), any(ReportConnectorDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api/connectors/conn-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(connectorService).updateConnector(eq("conn-123"), any(ReportConnectorDto.class));
    }

    @Test
    void deleteConnector_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/connectors/conn-123"))
                .andExpect(status().isNoContent());

        verify(connectorService).deleteConnector("conn-123");
    }

    @Test
    void testConnection_ShouldReturnOk() throws Exception {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        
        mockMvc.perform(post("/api/connectors/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(connectorService).testConnection(any(ReportConnectorDto.class));
    }
}
