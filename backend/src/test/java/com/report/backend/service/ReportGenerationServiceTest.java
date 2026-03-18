package com.report.backend.service;

import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import com.report.backend.entity.ReportTemplate;
import com.report.backend.entity.TemplateQueryMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReportGenerationServiceTest {

    @Mock
    private DatabaseExecutionService databaseExecutionService;

    @Mock
    private AsposeProcessingService asposeProcessingService;

    @Mock
    private ReportTemplateService reportTemplateService;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateReport_Success() throws Exception {
        // Mock template and mappings
        ReportTemplate template = new ReportTemplate();
        template.setId(1L);
        template.setName("Test Template");
        template.setFileData(new byte[]{1, 2, 3});

        ReportConnector connector = new ReportConnector();
        connector.setJdbcUrl("jdbc:mock:db");
        connector.setUsername("mockUser");
        connector.setPasswordEncrypted("mockPass");

        TemplateQueryMapping mapping1 = new TemplateQueryMapping();
        mapping1.setJsonNodeName("users");
        ReportQuery q1 = new ReportQuery();
        q1.setName("Query1");
        q1.setQueryText("SELECT 1 FROM users");
        q1.setConnector(connector);
        mapping1.setQuery(q1);

        TemplateQueryMapping mapping2 = new TemplateQueryMapping();
        mapping2.setJsonNodeName("sales");
        ReportQuery q2 = new ReportQuery();
        q2.setName("Query2");
        q2.setQueryText("SELECT 1 FROM sales");
        q2.setConnector(connector);
        mapping2.setQuery(q2);

        template.setMappings(Arrays.asList(mapping1, mapping2));

        when(reportTemplateService.getTemplateEntityById(1L)).thenReturn(template);

        // Mock JDBC execution results
        Map<String, Object> userRow = new HashMap<>();
        userRow.put("id", 1);
        userRow.put("name", "Alice");
        when(databaseExecutionService.executeQuery("Query1", "jdbc:mock:db", "mockUser", "mockPass", "SELECT 1 FROM users"))
                .thenReturn(List.of(userRow));

        Map<String, Object> saleRow = new HashMap<>();
        saleRow.put("amount", 100);
        when(databaseExecutionService.executeQuery("Query2", "jdbc:mock:db", "mockUser", "mockPass", "SELECT 1 FROM sales"))
                .thenReturn(List.of(saleRow));

        // Mock Aspose Process
        when(asposeProcessingService.processTemplate(any(byte[].class), anyString(), eq("PDF")))
                .thenReturn(new byte[]{9, 8, 7}); // Return mock PDF bytes

        // Execute
        byte[] result = reportGenerationService.generateReport(1L, "PDF");

        // Verify
        assertNotNull(result);
        verify(databaseExecutionService, times(2)).executeQuery(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(asposeProcessingService, times(1)).processTemplate(any(byte[].class), anyString(), eq("PDF"));
    }
}
