package com.report.backend.service;

import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.ReportTemplateVersionDto;
import com.report.backend.dto.TemplateQueryMappingDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceTest {

    @Mock
    private ReportTemplateService reportTemplateService;

    @Mock
    private ConnectorQueryServiceClient connectorQueryServiceClient;

    @Mock
    private AsposeProcessingService asposeProcessingService;

    @Mock
    private TemplateStorageStrategy storageStrategy;

    @InjectMocks
    private ReportGenerationService generationService;

    @Test
    void generateReport_Success() throws Exception {
        // Setup DTOs
        ReportTemplateDto templateDto = new ReportTemplateDto();
        templateDto.setId("t1");
        templateDto.setName("Test Template");

        ReportTemplateVersionDto versionDto = new ReportTemplateVersionDto();
        versionDto.setId("v1");
        versionDto.setVersionNumber(1);
        versionDto.setStoragePath("templates/test.docx");
        versionDto.setIsActive(1);

        TemplateQueryMappingDto mapping = new TemplateQueryMappingDto();
        mapping.setQueryId("q1");
        mapping.setJsonNodeName("data");
        versionDto.setMappings(Collections.singletonList(mapping));
        templateDto.setVersions(Collections.singletonList(versionDto));

        // Mocking
        when(reportTemplateService.getTemplateById("t1")).thenReturn(templateDto);
        when(storageStrategy.loadTemplate(anyString())).thenReturn(new byte[]{1, 2, 3});
        
        List<Map<String, Object>> queryResult = Collections.singletonList(new HashMap<>() {{ put("id", 1); }});
        when(connectorQueryServiceClient.executeQuery(eq("q1"), any())).thenReturn(queryResult);
        
        when(asposeProcessingService.processTemplate(any(), anyString(), anyString(), anyString()))
                .thenReturn(new byte[]{4, 5, 6});

        // Execution
        Map<String, Object> params = new HashMap<>();
        byte[] result = generationService.generateReport("t1", 1, "PDF", params);

        // Verification
        assertNotNull(result);
        assertArrayEquals(new byte[]{4, 5, 6}, result);
        verify(reportTemplateService).getTemplateById("t1");
        verify(storageStrategy).loadTemplate("templates/test.docx");
        verify(connectorQueryServiceClient).executeQuery(eq("q1"), eq(params));
    }

    @Test
    void generateReport_LatestVersion_Success() throws Exception {
        ReportTemplateDto templateDto = new ReportTemplateDto();
        templateDto.setId("t1");
        
        ReportTemplateVersionDto v1 = new ReportTemplateVersionDto();
        v1.setVersionNumber(1);
        v1.setIsActive(0);
        
        ReportTemplateVersionDto v2 = new ReportTemplateVersionDto();
        v2.setVersionNumber(2);
        v2.setIsActive(1); // Active
        v2.setStoragePath("path/v2.docx");
        v2.setMappings(Collections.emptyList());

        templateDto.setVersions(List.of(v1, v2));

        when(reportTemplateService.getTemplateById("t1")).thenReturn(templateDto);
        when(storageStrategy.loadTemplate("path/v2.docx")).thenReturn(new byte[]{0});
        when(asposeProcessingService.processTemplate(any(), anyString(), anyString(), anyString())).thenReturn(new byte[]{9});

        byte[] result = generationService.generateReport("t1", null, "DOCX", Collections.emptyMap());

        assertNotNull(result);
        verify(storageStrategy).loadTemplate("path/v2.docx");
    }

    @Test
    void generateReport_TemplateNotFound_ThrowsException() {
        when(reportTemplateService.getTemplateById("non-existent"))
                .thenThrow(new RuntimeException("Template not found"));

        assertThrows(RuntimeException.class, 
                () -> generationService.generateReport("non-existent", null, "PDF", Collections.emptyMap()));
    }
}
