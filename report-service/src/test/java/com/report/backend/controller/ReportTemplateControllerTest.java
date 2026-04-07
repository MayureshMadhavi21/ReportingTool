package com.report.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.PlaceholderMetadataDto;
import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.TemplateQueryMappingDto;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
                .andExpect(status().isOk());
    }

    @Test
    void updateTemplateFile_ShouldReturnTemplate() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.docx", MediaType.APPLICATION_OCTET_STREAM_VALUE, "content".getBytes());
        ReportTemplateDto resultDto = TestDataFactory.createTemplateDto();

        when(templateService.updateTemplateFile(eq("t1"), any())).thenReturn(resultDto);

        mockMvc.perform(multipart("/api/templates/t1/file").file(file).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());
    }

    @Test
    void addMapping_ShouldReturnMapping() throws Exception {
        TemplateQueryMappingDto dto = TestDataFactory.createMappingDto();
        dto.setTemplateId("t1"); // Match path variable
        when(templateService.addMapping(eq("t1"), any())).thenReturn(dto);

        mockMvc.perform(post("/api/templates/t1/mappings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void activateVersion_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/templates/versions/v1/activate"))
                .andExpect(status().isOk());
        verify(templateService).activateVersion("v1");
    }

    @Test
    void getPlaceholders_ShouldReturnList() throws Exception {
        when(templateService.getPlaceholdersForTemplate("t1")).thenReturn(Collections.singletonList(new PlaceholderMetadataDto()));
        mockMvc.perform(get("/api/templates/t1/placeholders"))
                .andExpect(status().isOk());
    }

    @Test
    void getTemplateFile_ShouldReturnByteArray() throws Exception {
        when(templateService.getTemplateFile("v1")).thenReturn("content".getBytes());
        when(templateService.getTemplateFilename("v1")).thenReturn("test.docx");

        mockMvc.perform(get("/api/templates/versions/v1/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.docx\""))
                .andExpect(content().bytes("content".getBytes()));
    }

    @Test
    void deleteTemplate_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/templates/temp-123"))
                .andExpect(status().isNoContent());

        verify(templateService).deleteTemplate("temp-123");
    }

    @Test
    void deleteMapping_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/templates/mappings/m1"))
                .andExpect(status().isNoContent());
        verify(templateService).deleteMapping("m1");
    }

    @Test
    void isQueryMappedToTemplate_ShouldReturnBoolean() throws Exception {
        when(templateService.isQueryMapped("q1")).thenReturn(true);
        mockMvc.perform(get("/api/templates/mapping-check?queryId=q1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
