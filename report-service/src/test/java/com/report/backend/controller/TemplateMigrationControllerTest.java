package com.report.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.MigrationDto;
import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.service.ReportTemplateService;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemplateMigrationController.class)
class TemplateMigrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportTemplateService templateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exportTemplate_ShouldReturnExportData() throws Exception {
        MigrationDto.TemplateExportDto exportDto = new MigrationDto.TemplateExportDto();
        when(templateService.exportTemplate("temp-123")).thenReturn(exportDto);

        mockMvc.perform(get("/api/migration/export/temp-123"))
                .andExpect(status().isOk());
    }

    @Test
    void analyzeImport_ShouldReturnAnalysis() throws Exception {
        MigrationDto.TemplateExportDto exportDto = new MigrationDto.TemplateExportDto();
        MigrationDto.MigrationAnalysisDto analysisDto = new MigrationDto.MigrationAnalysisDto();
        
        when(templateService.analyzeImport(any())).thenReturn(analysisDto);

        mockMvc.perform(post("/api/migration/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(exportDto)))
                .andExpect(status().isOk());
    }

    @Test
    void importTemplate_ShouldReturnImportedTemplate() throws Exception {
        MigrationDto.ImportRequestDto requestDto = new MigrationDto.ImportRequestDto();
        ReportTemplateDto templateDto = TestDataFactory.createTemplateDto();
        
        when(templateService.importTemplate(any())).thenReturn(templateDto);

        mockMvc.perform(post("/api/migration/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }
}
