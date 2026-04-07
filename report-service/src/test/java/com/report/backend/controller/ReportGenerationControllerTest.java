package com.report.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.ReportGenerationRequestDto;
import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.ReportTemplateVersionDto;
import com.report.backend.service.ReportGenerationService;
import com.report.backend.service.ReportTemplateService;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportGenerationController.class)
class ReportGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportGenerationService generationService;

    @MockBean
    private ReportTemplateService templateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateReport_Docx_ShouldReturnDocxBytes() throws Exception {
        ReportGenerationRequestDto request = TestDataFactory.createGenerationRequest();
        request.setFormat("DOCX");
        byte[] expectedBytes = new byte[]{1, 2, 3};

        ReportTemplateDto templateDto = TestDataFactory.createTemplateDto();
        ReportTemplateVersionDto versionDto = TestDataFactory.createVersionDto();
        versionDto.setStoragePath("test.docx");
        templateDto.setVersions(Collections.singletonList(versionDto));

        when(templateService.getTemplateById(anyString())).thenReturn(templateDto);
        when(generationService.generateReport(anyString(), any(), anyString(), anyMap()))
                .thenReturn(expectedBytes);

        mockMvc.perform(post("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(content().bytes(expectedBytes));
    }

    @Test
    void generateReport_Pdf_ShouldReturnPdfBytes() throws Exception {
        ReportGenerationRequestDto request = TestDataFactory.createGenerationRequest();
        request.setFormat("PDF");
        
        ReportTemplateDto templateDto = TestDataFactory.createTemplateDto();
        templateDto.setVersions(Collections.singletonList(new ReportTemplateVersionDto()));

        when(templateService.getTemplateById(anyString())).thenReturn(templateDto);
        when(generationService.generateReport(anyString(), any(), anyString(), anyMap()))
                .thenReturn(new byte[0]);

        mockMvc.perform(post("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void generateReport_Xlsx_ShouldReturnXlsxBytes() throws Exception {
        ReportGenerationRequestDto request = TestDataFactory.createGenerationRequest();
        request.setFormat("XLSX");
        
        ReportTemplateDto templateDto = TestDataFactory.createTemplateDto();
        ReportTemplateVersionDto versionDto = new ReportTemplateVersionDto();
        versionDto.setStoragePath("test.xlsx");
        templateDto.setVersions(Collections.singletonList(versionDto));

        when(templateService.getTemplateById(anyString())).thenReturn(templateDto);
        when(generationService.generateReport(anyString(), any(), anyString(), anyMap()))
                .thenReturn(new byte[0]);

        mockMvc.perform(post("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
