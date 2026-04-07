package com.report.backend.service;

import com.report.backend.dto.*;
import com.report.backend.entity.ReportTemplate;
import com.report.backend.entity.ReportTemplateVersion;
import com.report.backend.entity.TemplateQueryMapping;
import com.report.backend.repository.ReportTemplateRepository;
import com.report.backend.repository.ReportTemplateVersionRepository;
import com.report.backend.repository.TemplateQueryMappingRepository;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportTemplateServiceTest {

    @Mock
    private ReportTemplateRepository templateRepository;

    @Mock
    private ReportTemplateVersionRepository versionRepository;

    @Mock
    private ConnectorQueryServiceClient connectorQueryServiceClient;

    @Mock
    private TemplateQueryMappingRepository mappingRepository;

    @Mock
    private TemplateStorageStrategy storageStrategy;

    @InjectMocks
    private ReportTemplateService templateService;

    @Test
    void getAllTemplates_ShouldReturnDtoList() {
        ReportTemplate entity = TestDataFactory.createTemplateEntity();
        entity.setVersions(new ArrayList<>());
        when(templateRepository.findAll()).thenReturn(Collections.singletonList(entity));

        List<ReportTemplateDto> results = templateService.getAllTemplates();

        assertEquals(1, results.size());
    }

    @Test
    void uploadTemplate_ValidInput_ShouldSave() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.docx", "text/plain", "data".getBytes());
        ReportTemplate entity = TestDataFactory.createTemplateEntity();
        entity.setVersions(new ArrayList<>());

        when(templateRepository.save(any(ReportTemplate.class))).thenReturn(entity);
        when(storageStrategy.saveTemplate(anyString(), any(byte[].class), anyString())).thenReturn("/path");

        templateService.uploadTemplate("Name", "Desc", file);

        verify(templateRepository).save(any(ReportTemplate.class));
        verify(versionRepository).save(any(ReportTemplateVersion.class));
    }

    @Test
    void updateTemplateFile_ShouldCreateNewVersion() throws IOException {
        ReportTemplate template = TestDataFactory.createTemplateEntity();
        ReportTemplateVersion latest = new ReportTemplateVersion();
        latest.setVersionNumber(1);
        latest.setMappings(new ArrayList<>());
        
        when(templateRepository.findById("temp-123")).thenReturn(Optional.of(template));
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc("temp-123")).thenReturn(Optional.of(latest));
        when(versionRepository.findByTemplateId("temp-123")).thenReturn(Collections.singletonList(latest));
        when(storageStrategy.saveTemplate(anyString(), any(byte[].class), anyString())).thenReturn("/path2");

        MockMultipartFile file = new MockMultipartFile("file", "test2.docx", "text/plain", "data".getBytes());
        templateService.updateTemplateFile("temp-123", file);

        verify(versionRepository).save(argThat(v -> v.getVersionNumber() == 2));
    }

    @Test
    void deleteVersion_ActiveVersion_ShouldPromotePrevious() {
        ReportTemplateVersion target = new ReportTemplateVersion();
        target.setIsActive(1);
        target.setStoragePath("/path/to/v2");
        ReportTemplate template = new ReportTemplate();
        template.setId("t1");
        target.setTemplate(template);
        
        ReportTemplateVersion previous = new ReportTemplateVersion();
        previous.setVersionNumber(1);

        when(versionRepository.findById("v2")).thenReturn(Optional.of(target));
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc("t1")).thenReturn(Optional.of(previous));

        templateService.deleteVersion("v2");

        verify(versionRepository).delete(target);
        verify(versionRepository).save(argThat(v -> v.getIsActive() == 1));
        verify(storageStrategy).deleteTemplate("/path/to/v2");
    }

    @Test
    void addMapping_ValidInput_ShouldSave() {
        ReportTemplate template = TestDataFactory.createTemplateEntity();
        ReportTemplateVersion latest = new ReportTemplateVersion();
        latest.setTemplate(template);
        TemplateQueryMappingDto dto = TestDataFactory.createMappingDto();
        
        TemplateQueryMapping mappingResult = new TemplateQueryMapping();
        mappingResult.setTemplateVersion(latest);

        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc("temp-123")).thenReturn(Optional.of(latest));
        when(connectorQueryServiceClient.validateQueryExists("query-1")).thenReturn(true);
        when(mappingRepository.save(any(TemplateQueryMapping.class))).thenReturn(mappingResult);

        templateService.addMapping("temp-123", dto);

        verify(mappingRepository).save(any(TemplateQueryMapping.class));
    }

    @Test
    void updateMapping_ValidInput_ShouldSave() {
        TemplateQueryMapping mapping = new TemplateQueryMapping();
        ReportTemplateVersion latest = new ReportTemplateVersion();
        latest.setTemplate(TestDataFactory.createTemplateEntity());
        mapping.setTemplateVersion(latest);
        TemplateQueryMappingDto dto = TestDataFactory.createMappingDto();

        when(mappingRepository.findById("m1")).thenReturn(Optional.of(mapping));
        when(connectorQueryServiceClient.validateQueryExists(anyString())).thenReturn(true);
        when(mappingRepository.save(any(TemplateQueryMapping.class))).thenReturn(mapping);

        templateService.updateMapping("m1", dto);

        verify(mappingRepository).save(mapping);
    }

    @Test
    void deleteTemplate_ShouldCleanUpStorage() {
        ReportTemplate template = TestDataFactory.createTemplateEntity();
        ReportTemplateVersion v1 = new ReportTemplateVersion();
        v1.setStoragePath("/p1");
        template.setVersions(Collections.singletonList(v1));

        when(templateRepository.findById("t1")).thenReturn(Optional.of(template));
        templateService.deleteTemplate("t1");

        verify(storageStrategy).deleteTemplate("/p1");
        verify(templateRepository).delete(template);
    }

    @Test
    void activateVersion_ShouldSwitchActiveFlags() {
        ReportTemplateVersion target = new ReportTemplateVersion();
        target.setTemplate(new ReportTemplate());
        target.getTemplate().setId("t1");

        when(versionRepository.findById("v1")).thenReturn(Optional.of(target));
        when(versionRepository.findByTemplateId("t1")).thenReturn(Collections.singletonList(target));

        templateService.activateVersion("v1");

        verify(versionRepository).save(target);
        assertEquals(1, target.getIsActive());
    }

    @Test
    void getPlaceholdersForTemplate_ShouldCallClient() {
        ReportTemplateVersion v = new ReportTemplateVersion();
        TemplateQueryMapping m = new TemplateQueryMapping();
        m.setQueryId("q1");
        v.setMappings(Collections.singletonList(m));

        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc("t1")).thenReturn(Optional.of(v));
        when(connectorQueryServiceClient.getPlaceholders("q1")).thenReturn(Collections.singletonList(new PlaceholderMetadataDto()));

        List<PlaceholderMetadataDto> results = templateService.getPlaceholdersForTemplate("t1");

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void analyzeImport_ShouldCheckConnectorsAndQueries() {
        MigrationDto.TemplateExportDto export = new MigrationDto.TemplateExportDto();
        export.setTemplateName("Imported");
        MigrationDto.ExportedConnectorDto conn = new MigrationDto.ExportedConnectorDto();
        conn.setName("Conn1");
        export.setConnectors(Collections.singletonList(conn));
        export.setQueries(new ArrayList<>());
        export.setVersions(new ArrayList<>());

        when(connectorQueryServiceClient.getConnectorByName("Conn1")).thenReturn(Map.of("id", "real-id"));
        when(templateRepository.findByName("Imported")).thenReturn(Optional.empty());

        MigrationDto.MigrationAnalysisDto analysis = templateService.analyzeImport(export);

        assertTrue(analysis.getConnectors().get("Conn1").isExists());
    }

    @Test
    void importTemplate_SkipStrategy_ShouldUseExisting() {
        MigrationDto.ImportRequestDto request = new MigrationDto.ImportRequestDto();
        MigrationDto.TemplateExportDto data = new MigrationDto.TemplateExportDto();
        data.setTemplateName("T1");
        data.setVersions(new ArrayList<>());
        request.setExportData(data);
        
        MigrationDto.TemplateImportConfig tConfig = new MigrationDto.TemplateImportConfig();
        tConfig.setOriginalName("T1");
        tConfig.setStrategy("SKIP");
        request.setTemplate(tConfig);
        request.setConnectors(new ArrayList<>());
        request.setQueries(new ArrayList<>());

        ReportTemplate existing = new ReportTemplate();
        existing.setName("T1");
        existing.setVersions(new ArrayList<>());
        when(templateRepository.findByName("T1")).thenReturn(Optional.of(existing));

        templateService.importTemplate(request);

        verify(templateRepository, never()).save(any(ReportTemplate.class));
    }

    @Test
    void exportTemplate_ShouldEncodeFileContent() {
        ReportTemplate template = TestDataFactory.createTemplateEntity();
        ReportTemplateVersion v1 = new ReportTemplateVersion();
        v1.setVersionNumber(1);
        v1.setStoragePath("test.docx");
        v1.setMappings(new ArrayList<>());
        template.setVersions(Collections.singletonList(v1));

        when(templateRepository.findById("temp-123")).thenReturn(Optional.of(template));
        when(storageStrategy.loadTemplate("test.docx")).thenReturn("content".getBytes());

        MigrationDto.TemplateExportDto result = templateService.exportTemplate("temp-123");

        assertNotNull(result.getVersions().get(0).getFileContentBase64());
    }
}
