package com.report.backend.service;

import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.ReportTemplateVersionDto;
import com.report.backend.dto.TemplateQueryMappingDto;
import com.report.backend.entity.ReportTemplate;
import com.report.backend.entity.ReportTemplateVersion;
import com.report.backend.entity.TemplateQueryMapping;
import com.report.backend.repository.ReportTemplateRepository;
import com.report.backend.repository.ReportTemplateVersionRepository;
import com.report.backend.repository.TemplateQueryMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateVersionRepository versionRepository;
    private final ConnectorQueryServiceClient connectorQueryServiceClient;
    private final TemplateQueryMappingRepository mappingRepository;
    private final TemplateStorageStrategy storageStrategy;

    @Transactional(readOnly = true)
    public List<ReportTemplateDto> getAllTemplates() {
        return templateRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportTemplateDto getTemplateById(String id) {
        return templateRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    @Transactional(readOnly = true)
    public ReportTemplate getTemplateEntityById(String id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    @Transactional
    public ReportTemplateDto uploadTemplate(String name, String description, MultipartFile file) throws IOException {
        ReportTemplate template = new ReportTemplate();
        template.setName(name);
        template.setDescription(description);
        template = templateRepository.save(template);

        ReportTemplateVersion version = new ReportTemplateVersion();
        version.setTemplate(template);
        version.setVersionNumber(1);
        version.setCreatedBy("System User");
        version.setIsActive(1); // First upload is auto-active (1)

        String originalFilename = file.getOriginalFilename();
        String path = storageStrategy.saveTemplate(template.getId() + "_v1", file.getBytes(), originalFilename);
        version.setStoragePath(path);

        versionRepository.save(version);
        template.getVersions().add(version);

        return mapToDto(template);
    }

    @Transactional
    public ReportTemplateDto updateTemplateFile(String id, MultipartFile file) throws IOException {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        ReportTemplateVersion latest = versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(id)
                .orElseThrow(() -> new RuntimeException("No version found for template"));

        int nextVersionNumber = latest.getVersionNumber() + 1;

        // Deactivate all old versions
        versionRepository.findByTemplateId(id).forEach(v -> v.setIsActive(0));

        ReportTemplateVersion nextVersion = new ReportTemplateVersion();
        nextVersion.setTemplate(template);
        nextVersion.setVersionNumber(nextVersionNumber);
        nextVersion.setCreatedBy("System User");
        nextVersion.setIsActive(1); // New upload becomes active (1)

        String originalFilename = file.getOriginalFilename();
        String path = storageStrategy.saveTemplate(template.getId() + "_v" + nextVersionNumber, file.getBytes(),
                originalFilename);
        nextVersion.setStoragePath(path);

        // Clone mappings from latest version to next version
        for (TemplateQueryMapping oldMapping : latest.getMappings()) {
            TemplateQueryMapping newMapping = new TemplateQueryMapping();
            newMapping.setTemplateVersion(nextVersion);
            newMapping.setQueryId(oldMapping.getQueryId());
            newMapping.setQueryName(oldMapping.getQueryName());
            newMapping.setConnectorId(oldMapping.getConnectorId());
            newMapping.setConnectorName(oldMapping.getConnectorName());
            newMapping.setJsonNodeName(oldMapping.getJsonNodeName());
            nextVersion.getMappings().add(newMapping);
        }

        versionRepository.save(nextVersion);
        template.getVersions().add(nextVersion);

        return mapToDto(template);
    }

    @Transactional
    public void deleteTemplate(String id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        for (ReportTemplateVersion version : template.getVersions()) {
            storageStrategy.deleteTemplate(version.getStoragePath());
        }

        templateRepository.delete(template);
    }

    @Transactional
    public ReportTemplateDto updateTemplateInfo(String id, String name, String description) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        template.setName(name);
        template.setDescription(description);
        return mapToDto(templateRepository.save(template));
    }

    @Transactional
    public void activateVersion(String versionId) {
        ReportTemplateVersion target = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // Deactivate all other versions for this template
        versionRepository.findByTemplateId(target.getTemplate().getId()).forEach(v -> v.setIsActive(0));
        target.setIsActive(1);
        versionRepository.save(target);
    }

    @Transactional
    public void deleteVersion(String versionId) {
        ReportTemplateVersion target = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        boolean wasActive = Integer.valueOf(1).equals(target.getIsActive());
        String templateId = target.getTemplate().getId();

        storageStrategy.deleteTemplate(target.getStoragePath());
        versionRepository.delete(target);

        // Requirement: If latest version deleted, Previous version becomes latest
        if (wasActive) {
            versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(templateId).ifPresent(v -> {
                v.setIsActive(1);
                versionRepository.save(v);
            });
        }
    }

    @Transactional(readOnly = true)
    public Set<String> getPlaceholdersForVersion(String versionId) {
        ReportTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        Set<String> allPlaceholders = new HashSet<>();
        for (TemplateQueryMapping mapping : version.getMappings()) {
            Set<String> placeholders = connectorQueryServiceClient.getPlaceholders(mapping.getQueryId());
            if (placeholders != null) {
                allPlaceholders.addAll(placeholders);
            }
        }
        return allPlaceholders;
    }

    @Transactional
    public TemplateQueryMappingDto addMappingToVersion(String versionId, TemplateQueryMappingDto dto) {
        ReportTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        if (!connectorQueryServiceClient.validateQueryExists(dto.getQueryId())) {
            throw new RuntimeException("Query not found or connector-query-service unavailable");
        }

        Map<String, Object> queryDetails = connectorQueryServiceClient.getQueryDetails(dto.getQueryId());
        String queryName = queryDetails != null ? (String) queryDetails.get("name") : "Unknown Query";
        String connectorId = queryDetails != null ? (String) queryDetails.get("connectorId") : null;
        String connectorName = queryDetails != null ? (String) queryDetails.get("connectorName") : "Unknown Connector";

        TemplateQueryMapping mapping = new TemplateQueryMapping();
        mapping.setTemplateVersion(version);
        mapping.setQueryId(dto.getQueryId());
        mapping.setQueryName(queryName);
        mapping.setConnectorId(connectorId);
        mapping.setConnectorName(connectorName);
        mapping.setJsonNodeName(dto.getJsonNodeName());

        return mapMappingToDto(mappingRepository.save(mapping));
    }

    @Transactional
    public TemplateQueryMappingDto addMapping(String templateId, TemplateQueryMappingDto dto) {
        ReportTemplateVersion latest = versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(templateId)
                .orElseThrow(() -> new RuntimeException("No version found for template"));

        if (!connectorQueryServiceClient.validateQueryExists(dto.getQueryId())) {
            throw new RuntimeException("Query not found or connector-query-service unavailable");
        }
        Map<String, Object> queryDetails = connectorQueryServiceClient.getQueryDetails(dto.getQueryId());
        String queryName = queryDetails != null ? (String) queryDetails.get("name") : "Unknown Query";
        String connectorId = queryDetails != null ? (String) queryDetails.get("connectorId") : null;
        String connectorName = queryDetails != null ? (String) queryDetails.get("connectorName") : "Unknown Connector";

        TemplateQueryMapping mapping = new TemplateQueryMapping();
        mapping.setTemplateVersion(latest);
        mapping.setQueryId(dto.getQueryId());
        mapping.setQueryName(queryName);
        mapping.setConnectorId(connectorId);
        mapping.setConnectorName(connectorName);
        mapping.setJsonNodeName(dto.getJsonNodeName());

        return mapMappingToDto(mappingRepository.save(mapping));
    }

    @Transactional
    public TemplateQueryMappingDto updateMapping(String mappingId, TemplateQueryMappingDto dto) {
        TemplateQueryMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found"));

        if (!connectorQueryServiceClient.validateQueryExists(dto.getQueryId())) {
            throw new RuntimeException("Query not found or connector-query-service unavailable");
        }

        Map<String, Object> queryDetails = connectorQueryServiceClient.getQueryDetails(dto.getQueryId());
        String queryName = queryDetails != null ? (String) queryDetails.get("name") : "Unknown Query";
        String connectorId = queryDetails != null ? (String) queryDetails.get("connectorId") : null;
        String connectorName = queryDetails != null ? (String) queryDetails.get("connectorName") : "Unknown Connector";

        mapping.setQueryId(dto.getQueryId());
        mapping.setQueryName(queryName);
        mapping.setConnectorId(connectorId);
        mapping.setConnectorName(connectorName);
        mapping.setJsonNodeName(dto.getJsonNodeName());

        return mapMappingToDto(mappingRepository.save(mapping));
    }

    @Transactional
    public void deleteMapping(String mappingId) {
        mappingRepository.deleteById(mappingId);
    }

    @Transactional(readOnly = true)
    public Set<String> getPlaceholdersForTemplate(String templateId) {
        ReportTemplateVersion latest = versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(templateId)
                .orElseThrow(() -> new RuntimeException("No version found for template"));

        Set<String> allPlaceholders = new HashSet<>();
        for (TemplateQueryMapping mapping : latest.getMappings()) {
            Set<String> placeholders = connectorQueryServiceClient.getPlaceholders(mapping.getQueryId());
            if (placeholders != null) {
                allPlaceholders.addAll(placeholders);
            }
        }
        return allPlaceholders;
    }

    @Transactional(readOnly = true)
    public boolean isQueryMapped(String queryId) {
        return mappingRepository.existsByQueryId(queryId);
    }

    @Transactional(readOnly = true)
    public byte[] getTemplateFile(String versionId) {
        ReportTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return storageStrategy.loadTemplate(version.getStoragePath());
    }

    @Transactional(readOnly = true)
    public String getTemplateFilename(String versionId) {
        ReportTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        return version.getStoragePath(); // This contains the filename
    }

    private ReportTemplateDto mapToDto(ReportTemplate entity) {
        ReportTemplateDto dto = new ReportTemplateDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());

        List<ReportTemplateVersionDto> versions = entity.getVersions().stream()
                .map(this::mapVersionToDto)
                .sorted((v1, v2) -> v2.getVersionNumber().compareTo(v1.getVersionNumber()))
                .collect(Collectors.toList());

        dto.setVersions(versions);
        if (!versions.isEmpty()) {
            dto.setLatestVersionNumber(versions.get(0).getVersionNumber());
        }

        return dto;
    }

    private ReportTemplateVersionDto mapVersionToDto(ReportTemplateVersion version) {
        ReportTemplateVersionDto dto = new ReportTemplateVersionDto();
        dto.setId(version.getId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setStoragePath(version.getStoragePath());

        String path = version.getStoragePath() != null ? version.getStoragePath().toLowerCase() : "";
        dto.setFileType(path.endsWith(".xlsx") ? "XLSX" : "DOCX");

        dto.setCreatedBy(version.getCreatedBy());
        dto.setIsActive(version.getIsActive() != null ? version.getIsActive() : 0);
        dto.setCreatedAt(version.getCreatedAt());
        dto.setMappings(version.getMappings().stream()
                .map(this::mapMappingToDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private TemplateQueryMappingDto mapMappingToDto(TemplateQueryMapping mapping) {
        TemplateQueryMappingDto dto = new TemplateQueryMappingDto();
        dto.setId(mapping.getId());
        dto.setTemplateId(mapping.getTemplateVersion().getTemplate().getId());
        dto.setQueryId(mapping.getQueryId());
        dto.setQueryName(mapping.getQueryName());
        dto.setConnectorId(mapping.getConnectorId());
        dto.setConnectorName(mapping.getConnectorName());
        dto.setJsonNodeName(mapping.getJsonNodeName());
        return dto;
    }
}
