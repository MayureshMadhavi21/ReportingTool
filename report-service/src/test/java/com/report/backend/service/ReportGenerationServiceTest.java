package com.report.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.ReportTemplateVersionDto;
import com.report.backend.dto.TemplateQueryMappingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportGenerationServiceTest {

    @Mock
    private ConnectorQueryServiceClient connectorQueryServiceClient;

    @Mock
    private AsposeProcessingService asposeProcessingService;

    @Mock
    private ReportTemplateService reportTemplateService;
    
    @Mock
    private TemplateStorageStrategy templateStorageStrategy;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateReport_Success() throws Exception {
        String templateId = "template-uuid-001";
        
        // Mock template and versions
        ReportTemplateDto template = new ReportTemplateDto();
        template.setId(templateId);
        template.setName("Test Template");

        ReportTemplateVersionDto version = new ReportTemplateVersionDto();
        version.setVersionNumber(1);
        version.setStoragePath("path/to/template.docx");
        
        List<TemplateQueryMappingDto> mappings = new ArrayList<>();
        
        TemplateQueryMappingDto mapping1 = new TemplateQueryMappingDto();
        mapping1.setJsonNodeName("users");
        mapping1.setQueryId("query-uuid-101");
        mapping1.setQueryName("Query1");
        mappings.add(mapping1);

        TemplateQueryMappingDto mapping2 = new TemplateQueryMappingDto();
        mapping2.setJsonNodeName("sales");
        mapping2.setQueryId("query-uuid-102");
        mapping2.setQueryName("Query2");
        mappings.add(mapping2);

        version.setMappings(mappings);
        template.setVersions(List.of(version));

        when(reportTemplateService.getTemplateById(templateId)).thenReturn(template);
        
        // Mock storage strategy
        when(templateStorageStrategy.loadTemplate(anyString())).thenReturn(new byte[]{1, 2, 3});

        Map<String, Object> testParams = new HashMap<>();
        testParams.put("p1", "v1");

        // Mock remote execution results
        Map<String, Object> userRow = new HashMap<>();
        userRow.put("id", 1);
        userRow.put("name", "Alice");
        when(connectorQueryServiceClient.executeQuery(eq("query-uuid-101"), anyMap()))
                .thenReturn(List.of(userRow));

        Map<String, Object> saleRow = new HashMap<>();
        saleRow.put("amount", 100);
        when(connectorQueryServiceClient.executeQuery(eq("query-uuid-102"), anyMap()))
                .thenReturn(List.of(saleRow));

        // Mock Aspose Process
        when(asposeProcessingService.processTemplate(any(byte[].class), anyString(), anyString(), eq("DOCX")))
                .thenReturn(new byte[]{9, 8, 7}); 

        // Execute (using null version to pick latest)
        byte[] result = reportGenerationService.generateReport(templateId, null, "DOCX", testParams);

        // Verify
        assertNotNull(result);
        verify(connectorQueryServiceClient, times(2)).executeQuery(anyString(), anyMap());
        verify(asposeProcessingService, times(1)).processTemplate(any(byte[].class), anyString(), anyString(), eq("DOCX"));
    }
}
