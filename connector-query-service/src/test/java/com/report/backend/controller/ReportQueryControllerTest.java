package com.report.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.ReportQueryDto;
import com.report.backend.service.ReportQueryService;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportQueryController.class)
class ReportQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportQueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllQueries_ShouldReturnList() throws Exception {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        when(queryService.getAllQueries()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(dto.getId()))
                .andExpect(jsonPath("$[0].name").value(dto.getName()));
    }

    @Test
    void getQueriesByConnector_ShouldReturnList() throws Exception {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        when(queryService.getQueriesByConnector("conn-1")).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/queries/connector/conn-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(dto.getId()));
    }

    @Test
    void getQueryById_Found_ShouldReturnDto() throws Exception {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        when(queryService.getQueryById("query-123")).thenReturn(dto);

        mockMvc.perform(get("/api/queries/query-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dto.getId()));
    }

    @Test
    void getQueryByName_Found_ShouldReturnDto() throws Exception {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        dto.setName("q1"); // Match search parameter
        when(queryService.getQueryByConnectorAndName("c1", "q1")).thenReturn(dto);

        mockMvc.perform(get("/api/queries/search?connectorId=c1&name=q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("q1"));
    }

    @Test
    void getQueryByName_NotFound_ShouldReturn404() throws Exception {
        when(queryService.getQueryByConnectorAndName(anyString(), anyString())).thenReturn(null);

        mockMvc.perform(get("/api/queries/search?connectorId=c1&name=invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createQuery_ValidInput_ShouldReturnCreated() throws Exception {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        when(queryService.createQuery(any(ReportQueryDto.class))).thenReturn(dto);

        mockMvc.perform(post("/api/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dto.getId()));

        verify(queryService).createQuery(any(ReportQueryDto.class));
    }

    @Test
    void updateQuery_ValidInput_ShouldReturnUpdated() throws Exception {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        when(queryService.updateQuery(eq("query-123"), any(ReportQueryDto.class))).thenReturn(dto);

        mockMvc.perform(put("/api/queries/query-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(queryService).updateQuery(eq("query-123"), any(ReportQueryDto.class));
    }

    @Test
    void validateQuery_ShouldReturnOk() throws Exception {
        ReportQueryDto dto = TestDataFactory.createQueryDto();
        
        mockMvc.perform(post("/api/queries/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(queryService).validateQuerySyntax(any(ReportQueryDto.class));
    }

    @Test
    void executeQuery_ShouldReturnResults() throws Exception {
        List<Map<String, Object>> results = Collections.singletonList(new HashMap<>(Map.of("column1", "value1")));
        when(queryService.executeQuery(eq("query-123"), anyMap())).thenReturn(results);

        mockMvc.perform(post("/api/queries/query-123/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].column1").value("value1"));

        verify(queryService).executeQuery(eq("query-123"), anyMap());
    }

    @Test
    void getPlaceholders_ShouldReturnList() throws Exception {
        when(queryService.getPlaceholders("query-123")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/queries/query-123/placeholders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(queryService).getPlaceholders("query-123");
    }

    @Test
    void deleteQuery_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/queries/query-123"))
                .andExpect(status().isNoContent());

        verify(queryService).deleteQuery("query-123");
    }
}
