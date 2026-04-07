package com.report.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.service.ReportTemplateService;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportTemplateController.class)
class ReportTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportTemplateService templateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllTemplates_ShouldReturnList() throws Exception {
        ReportTemplateDto dto = TestDataFactory.createTemplateDto();
        when(templateService.getAllTemplates()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(dto.getName()));
    }

    @Test
    void getTemplateById_Found_ShouldReturnDto() throws Exception {
        ReportTemplateDto dto = TestDataFactory.createTemplateDto();
        when(templateService.getTemplateById("temp-123")).thenReturn(dto);

        mockMvc.perform(get("/api/templates/temp-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("temp-123"));
    }

    @Test
    void uploadTemplate_ValidInput_ShouldReturnTemplate() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.docx", MediaType.APPLICATION_OCTET_STREAM_VALUE, "content".getBytes());
        ReportTemplateDto resultDto = TestDataFactory.createTemplateDto();

        when(templateService.uploadTemplate(anyString(), anyString(), any())).thenReturn(resultDto);

        mockMvc.perform(multipart("/api/templates")
                .file(file)
                .param("name", "Test Name")
                .param("description", "Test Desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(resultDto.getName()));
    }

    @Test
    void updateTemplateInfo_ValidInput_ShouldReturnUpdated() throws Exception {
        ReportTemplateDto dto = TestDataFactory.createTemplateDto();
        dto.setName("Updated Name");
        dto.setDescription("Updated Desc");

        when(templateService.updateTemplateInfo(eq("temp-123"), anyString(), anyString())).thenReturn(dto);

        mockMvc.perform(put("/api/templates/temp-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void deleteTemplate_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/templates/temp-123"))
                .andExpect(status().isNoContent());

        verify(templateService).deleteTemplate("temp-123");
    }
}
