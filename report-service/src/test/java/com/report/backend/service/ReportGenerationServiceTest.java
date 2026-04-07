package com.report.backend.service;

import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.ReportTemplateVersionDto;
import com.report.backend.dto.TemplateQueryMappingDto;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Test
    void generateReport_Success_ShouldReturnBytes() throws Exception {
        String templateId = "temp-123";
        Map<String, Object> testParams = new HashMap<>();
        
        ReportTemplateDto template = TestDataFactory.createTemplateDto();
        ReportTemplateVersionDto version = TestDataFactory.createVersionDto();
        version.setIsActive(1);
        TemplateQueryMappingDto mapping = TestDataFactory.createMappingDto();
        version.setMappings(Collections.singletonList(mapping));
        template.setVersions(Collections.singletonList(version));

        when(reportTemplateService.getTemplateById(templateId)).thenReturn(template);
        when(connectorQueryServiceClient.executeQuery(anyString(), anyMap()))
                .thenReturn(Collections.singletonList(Map.of("data", "value")));
        when(templateStorageStrategy.loadTemplate(anyString())).thenReturn("templateData".getBytes());
        when(asposeProcessingService.processTemplate(any(), anyString(), anyString(), anyString()))
                .thenReturn("pdfContent".getBytes());

        byte[] result = reportGenerationService.generateReport(templateId, null, "PDF", testParams);

        assertNotNull(result);
        verify(asposeProcessingService).processTemplate(any(), contains("dataNode"), eq("DOCX"), eq("PDF"));
    }

    @Test
    void generateReport_NoVersions_ThrowsException() {
        ReportTemplateDto template = TestDataFactory.createTemplateDto();
        template.setVersions(new ArrayList<>());
        when(reportTemplateService.getTemplateById("t1")).thenReturn(template);

        assertThrows(RuntimeException.class, () -> reportGenerationService.generateReport("t1", null, "PDF", null));
    }

    @Test
    void generateReport_SpecificVersion_ShouldUseIt() throws Exception {
        ReportTemplateDto template = TestDataFactory.createTemplateDto();
        ReportTemplateVersionDto v1 = TestDataFactory.createVersionDto();
        v1.setVersionNumber(1);
        ReportTemplateVersionDto v2 = TestDataFactory.createVersionDto();
        v2.setVersionNumber(2);
        template.setVersions(Arrays.asList(v1, v2));

        when(reportTemplateService.getTemplateById("t1")).thenReturn(template);
        when(templateStorageStrategy.loadTemplate(anyString())).thenReturn(new byte[0]);
        when(asposeProcessingService.processTemplate(any(), anyString(), anyString(), anyString())).thenReturn(new byte[0]);

        reportGenerationService.generateReport("t1", 2, "PDF", new HashMap<>());

        verify(templateStorageStrategy).loadTemplate(v2.getStoragePath());
    }

    @Test
    void generateReport_ExecutionError_ThrowsException() {
        ReportTemplateDto template = TestDataFactory.createTemplateDto();
        ReportTemplateVersionDto v1 = TestDataFactory.createVersionDto();
        v1.setIsActive(1);
        v1.setMappings(Collections.singletonList(TestDataFactory.createMappingDto()));
        template.setVersions(Collections.singletonList(v1));

        when(reportTemplateService.getTemplateById("t1")).thenReturn(template);
        when(connectorQueryServiceClient.executeQuery(anyString(), anyMap())).thenThrow(new RuntimeException("API Down"));

        // Parallel execution uses CompletableFuture.supplyAsync which wraps in CompletionException if join() is called
        // In the code, it's join()ed and then future.get() is called inside a loop with catch blocks.
        assertThrows(RuntimeException.class, () -> reportGenerationService.generateReport("t1", null, "PDF", null));
    }
}
