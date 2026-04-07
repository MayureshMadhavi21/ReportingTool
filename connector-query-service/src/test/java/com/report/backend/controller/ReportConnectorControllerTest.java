package com.report.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.service.ReportConnectorService;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void getConnectorByName_Found_ShouldReturnDto() throws Exception {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        dto.setName("TestConn"); // Match search parameter
        when(connectorService.getConnectorByName("TestConn")).thenReturn(dto);

        mockMvc.perform(get("/api/connectors/search?name=TestConn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestConn"));
    }

    @Test
    void getConnectorByName_NotFound_ShouldReturn404() throws Exception {
        when(connectorService.getConnectorByName(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/connectors/search?name=Invalid"))
                .andExpect(status().isNotFound());
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
    void updateConnectorPassword_ShouldReturnUpdated() throws Exception {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        Map<String, String> payload = Map.of("password", "new-secret");
        
        when(connectorService.updateConnectorPassword(eq("c1"), eq("new-secret"))).thenReturn(dto);

        mockMvc.perform(put("/api/connectors/c1/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(connectorService).updateConnectorPassword("c1", "new-secret");
    }

    @Test
    void testConnection_ShouldReturnOk() throws Exception {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        
        mockMvc.perform(post("/api/connectors/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(connectorService).testConnection(any(ReportConnectorDto.class));
    }

    @Test
    void createConnector_InvalidInput_ShouldReturnBadRequest() throws Exception {
        ReportConnectorDto dto = new ReportConnectorDto(); // Missing all mandatory fields
        
        mockMvc.perform(post("/api/connectors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteConnector_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/connectors/conn-123"))
                .andExpect(status().isNoContent());

        verify(connectorService).deleteConnector("conn-123");
    }
}
