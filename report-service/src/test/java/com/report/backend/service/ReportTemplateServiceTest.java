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
    void updateTemplateFile_WithMappings_ShouldCloneMappings() throws IOException {
        ReportTemplate template = TestDataFactory.createTemplateEntity();
        ReportTemplateVersion latest = new ReportTemplateVersion();
        latest.setVersionNumber(1);
        TemplateQueryMapping m1 = new TemplateQueryMapping();
        m1.setQueryId("q1");
        m1.setJsonNodeName("node1");
        latest.setMappings(new ArrayList<>(List.of(m1)));
        
        when(templateRepository.findById("temp-123")).thenReturn(Optional.of(template));
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc("temp-123")).thenReturn(Optional.of(latest));
        when(versionRepository.findByTemplateId("temp-123")).thenReturn(List.of(latest));
        when(storageStrategy.saveTemplate(anyString(), any(), any())).thenReturn("/p2");

        MockMultipartFile file = new MockMultipartFile("file", "t2.docx", "text/plain", "data".getBytes());
        templateService.updateTemplateFile("temp-123", file);

        verify(versionRepository).save(argThat(v -> v.getMappings().size() == 1 && v.getMappings().get(0).getJsonNodeName().equals("node1")));
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
    void deleteVersion_ActiveVersion_ShouldPromotePrevious() {
        ReportTemplateVersion target = new ReportTemplateVersion();
        target.setId("v2");
        target.setIsActive(1);
        target.setStoragePath("/path/to/v2");
        ReportTemplate template = new ReportTemplate();
        template.setId("t1");
        target.setTemplate(template);
        
        ReportTemplateVersion previous = new ReportTemplateVersion();
        previous.setId("v1");
        previous.setVersionNumber(1);
        previous.setIsActive(0);

        when(versionRepository.findById("v2")).thenReturn(Optional.of(target));
        when(versionRepository.findTopByTemplateIdOrderByVersionNumberDesc("t1")).thenReturn(Optional.of(previous));

        templateService.deleteVersion("v2");

        verify(versionRepository).delete(target);
        verify(versionRepository).save(argThat(v -> v.getIsActive() == 1));
        verify(storageStrategy).deleteTemplate("/path/to/v2");
    }

    @Test
    void deleteVersion_InactiveVersion_ShouldNotPromote() {
        ReportTemplateVersion target = new ReportTemplateVersion();
        target.setId("v2");
        target.setIsActive(0);
        target.setStoragePath("/path/v2");
        target.setTemplate(new ReportTemplate());
        target.getTemplate().setId("t1");

        when(versionRepository.findById("v2")).thenReturn(Optional.of(target));

        templateService.deleteVersion("v2");

        verify(versionRepository).delete(target);
        verify(versionRepository, never()).save(any());
    }

    @Test
    void getPlaceholdersForVersion_Complex_ShouldAggregate() {
        ReportTemplateVersion v = new ReportTemplateVersion();
        TemplateQueryMapping m1 = new TemplateQueryMapping();
        m1.setQueryId("q1");
        TemplateQueryMapping m2 = new TemplateQueryMapping();
        m2.setQueryId("q2");
        v.setMappings(List.of(m1, m2));

        PlaceholderMetadataDto p1 = new PlaceholderMetadataDto();
        p1.setName("date");
        PlaceholderMetadataDto p2 = new PlaceholderMetadataDto();
        p2.setName("limit");

        when(versionRepository.findById("v1")).thenReturn(Optional.of(v));
        when(connectorQueryServiceClient.getPlaceholders("q1")).thenReturn(List.of(p1));
        when(connectorQueryServiceClient.getPlaceholders("q2")).thenReturn(List.of(p1, p2));

        List<PlaceholderMetadataDto> results = templateService.getPlaceholdersForVersion("v1");

        assertEquals(2, results.size());
    }

    @Test
    void importTemplate_Override_ShouldDeleteOldAndSaveNew() {
        MigrationDto.ImportRequestDto request = new MigrationDto.ImportRequestDto();
        MigrationDto.TemplateExportDto data = new MigrationDto.TemplateExportDto();
        data.setTemplateName("T1");
        data.setDescription("New Desc");
        data.setConnectors(new ArrayList<>());
        data.setQueries(new ArrayList<>());
        
        MigrationDto.ExportedVersionDto vExport = new MigrationDto.ExportedVersionDto();
        vExport.setVersionNumber(1);
        vExport.setFileContentBase64(Base64.getEncoder().encodeToString("test".getBytes()));
        vExport.setMappings(new ArrayList<>());
        data.setVersions(Collections.singletonList(vExport));
        request.setExportData(data);

        MigrationDto.TemplateImportConfig tConfig = new MigrationDto.TemplateImportConfig();
        tConfig.setOriginalName("T1");
        tConfig.setStrategy("OVERRIDE");
        request.setTemplate(tConfig);
        request.setConnectors(new ArrayList<>());
        request.setQueries(new ArrayList<>());

        ReportTemplate existing = new ReportTemplate();
        existing.setName("T1");
        ReportTemplateVersion oldV = new ReportTemplateVersion();
        oldV.setStoragePath("old.docx");
        existing.setVersions(new ArrayList<>(List.of(oldV)));
        
        when(templateRepository.findByName("T1")).thenReturn(Optional.of(existing));
        when(templateRepository.save(any())).thenReturn(existing);
        when(storageStrategy.saveTemplate(anyString(), any(), any())).thenReturn("new.docx");

        templateService.importTemplate(request);

        verify(storageStrategy).deleteTemplate("old.docx");
        verify(versionRepository).delete(oldV);
    }

    @Test
    void importTemplate_CreateNew_Conflict_ShouldThrowException() {
        MigrationDto.ImportRequestDto request = new MigrationDto.ImportRequestDto();
        MigrationDto.TemplateExportDto data = new MigrationDto.TemplateExportDto();
        data.setTemplateName("T1");
        request.setExportData(data);
        request.setConnectors(new ArrayList<>());
        request.setQueries(new ArrayList<>());
        
        MigrationDto.TemplateImportConfig tConfig = new MigrationDto.TemplateImportConfig();
        tConfig.setTargetName("ExistingName");
        tConfig.setStrategy("CREATE_NEW");
        request.setTemplate(tConfig);

        lenient().when(templateRepository.findByName("ExistingName")).thenReturn(Optional.of(new ReportTemplate()));

        assertThrows(RuntimeException.class, () -> templateService.importTemplate(request));
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
}
