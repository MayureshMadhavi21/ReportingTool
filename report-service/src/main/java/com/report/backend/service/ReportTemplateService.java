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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.report.backend.dto.MigrationDto;

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
            newMapping.setConnectorDbType(oldMapping.getConnectorDbType());
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
    public List<com.report.backend.dto.PlaceholderMetadataDto> getPlaceholdersForVersion(String versionId) {
        ReportTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        Map<String, com.report.backend.dto.PlaceholderMetadataDto> allPlaceholders = new HashMap<>();
        for (TemplateQueryMapping mapping : version.getMappings()) {
            List<com.report.backend.dto.PlaceholderMetadataDto> placeholders = connectorQueryServiceClient.getPlaceholders(mapping.getQueryId());
            if (placeholders != null) {
                for (com.report.backend.dto.PlaceholderMetadataDto p : placeholders) {
                    allPlaceholders.putIfAbsent(p.getName(), p);
                }
            }
        }
        return new ArrayList<>(allPlaceholders.values());
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
        String connectorDbType = queryDetails != null ? (String) queryDetails.get("connectorDbType") : "H2"; // Fallback to current default if unknown

        TemplateQueryMapping mapping = new TemplateQueryMapping();
        mapping.setTemplateVersion(version);
        mapping.setQueryId(dto.getQueryId());
        mapping.setQueryName(queryName);
        mapping.setConnectorId(connectorId);
        mapping.setConnectorName(connectorName);
        mapping.setConnectorDbType(connectorDbType);
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
        String connectorDbType = queryDetails != null ? (String) queryDetails.get("connectorDbType") : "H2";

        TemplateQueryMapping mapping = new TemplateQueryMapping();
        mapping.setTemplateVersion(latest);
        mapping.setQueryId(dto.getQueryId());
        mapping.setQueryName(queryName);
        mapping.setConnectorId(connectorId);
        mapping.setConnectorName(connectorName);
        mapping.setConnectorDbType(connectorDbType);
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
        String connectorDbType = queryDetails != null ? (String) queryDetails.get("connectorDbType") : "H2";

        mapping.setQueryId(dto.getQueryId());
        mapping.setQueryName(queryName);
        mapping.setConnectorId(connectorId);
        mapping.setConnectorName(connectorName);
        mapping.setConnectorDbType(connectorDbType);
        mapping.setJsonNodeName(dto.getJsonNodeName());

        return mapMappingToDto(mappingRepository.save(mapping));
    }

    @Transactional
    public void deleteMapping(String mappingId) {
        mappingRepository.deleteById(mappingId);
    }

    @Transactional(readOnly = true)
    public List<com.report.backend.dto.PlaceholderMetadataDto> getPlaceholdersForTemplate(String templateId) {
        ReportTemplateVersion latest = versionRepository.findTopByTemplateIdOrderByVersionNumberDesc(templateId)
                .orElseThrow(() -> new RuntimeException("No version found for template"));

        Map<String, com.report.backend.dto.PlaceholderMetadataDto> allPlaceholders = new HashMap<>();
        for (TemplateQueryMapping mapping : latest.getMappings()) {
            List<com.report.backend.dto.PlaceholderMetadataDto> placeholders = connectorQueryServiceClient.getPlaceholders(mapping.getQueryId());
            if (placeholders != null) {
                for (com.report.backend.dto.PlaceholderMetadataDto p : placeholders) {
                    allPlaceholders.putIfAbsent(p.getName(), p);
                }
            }
        }
        return new ArrayList<>(allPlaceholders.values());
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

    @Transactional(readOnly = true)
    public MigrationDto.MigrationAnalysisDto analyzeImport(MigrationDto.TemplateExportDto data) {
        MigrationDto.MigrationAnalysisDto analysis = new MigrationDto.MigrationAnalysisDto();

        // --- Analyze Connectors ---
        Map<String, MigrationDto.ConnectorAnalysis> connectorMap = new HashMap<>();
        Map<String, List<String>> connectorImpactMap = new HashMap<>();

        for (MigrationDto.ExportedConnectorDto c : data.getConnectors()) {
            MigrationDto.ConnectorAnalysis ca = new MigrationDto.ConnectorAnalysis();
            Map<String, Object> existing = connectorQueryServiceClient.getConnectorByName(c.getName());
            if (existing != null && existing.get("id") != null) {
                ca.setExists(true);
                ca.setExistingId((String) existing.get("id"));
            } else {
                ca.setExists(false);
            }
            connectorMap.put(c.getName(), ca);

            // Build impact: which queries from this export use this connector
            List<String> affectedQueryNames = data.getQueries().stream()
                    .filter(q -> c.getName().equals(q.getConnectorName()))
                    .map(MigrationDto.ExportedQueryDto::getName)
                    .collect(Collectors.toList());
            connectorImpactMap.put(c.getName(), affectedQueryNames);
        }

        // --- Analyze Queries ---
        Map<String, MigrationDto.QueryAnalysis> queryMap = new HashMap<>();
        Map<String, List<String>> queryImpactMap = new HashMap<>();

        for (MigrationDto.ExportedQueryDto q : data.getQueries()) {
            MigrationDto.QueryAnalysis qa = new MigrationDto.QueryAnalysis();

            // Find the connector ID for this query's connector
            MigrationDto.ConnectorAnalysis ca = connectorMap.get(q.getConnectorName());
            String connectorId = (ca != null && ca.isExists()) ? ca.getExistingId() : null;

            Map<String, Object> existingQuery = null;
            if (connectorId != null) {
                existingQuery = connectorQueryServiceClient.getQueryByName(connectorId, q.getName());
            }

            if (existingQuery != null && existingQuery.get("id") != null) {
                qa.setExists(true);
                qa.setExistingId((String) existingQuery.get("id"));
                // Option B: fetch current query text for SQL diff view
                qa.setCurrentQueryText(existingQuery.get("queryText") != null
                        ? (String) existingQuery.get("queryText") : "");
            } else {
                qa.setExists(false);
            }
            queryMap.put(q.getName(), qa);

            // Build impact: which template versions (from export) reference this query
            List<String> affectedTemplateVersions = new ArrayList<>();
            for (MigrationDto.ExportedVersionDto v : data.getVersions()) {
                boolean used = v.getMappings().stream()
                        .anyMatch(m -> q.getName().equals(m.getQueryName()));
                if (used) {
                    affectedTemplateVersions.add("v" + v.getVersionNumber() + " of '" + data.getTemplateName() + "'");
                }
            }
            queryImpactMap.put(q.getName(), affectedTemplateVersions);
        }

        // --- Analyze Template ---
        boolean templateExists = templateRepository.findByName(data.getTemplateName()).isPresent();

        analysis.setConnectors(connectorMap);
        analysis.setQueries(queryMap);
        analysis.setTemplateExists(templateExists);
        analysis.setConnectorImpactMap(connectorImpactMap);
        analysis.setQueryImpactMap(queryImpactMap);
        return analysis;
    }

    @Transactional(readOnly = true)
    public MigrationDto.TemplateExportDto exportTemplate(String templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        MigrationDto.TemplateExportDto export = new MigrationDto.TemplateExportDto();
        export.setTemplateName(template.getName());
        export.setDescription(template.getDescription());

        List<MigrationDto.ExportedConnectorDto> connectors = new ArrayList<>();
        List<MigrationDto.ExportedQueryDto> queries = new ArrayList<>();
        List<MigrationDto.ExportedVersionDto> versions = new ArrayList<>();

        Set<String> processedConnectorIds = new HashSet<>();
        Set<String> processedQueryIds = new HashSet<>();

        for (ReportTemplateVersion version : template.getVersions()) {
            MigrationDto.ExportedVersionDto vDto = new MigrationDto.ExportedVersionDto();
            vDto.setVersionNumber(version.getVersionNumber());
            vDto.setIsActive(version.getIsActive());
            vDto.setOriginalPath(version.getStoragePath());
            
            // Extract filename from path for debugging visibility
            String fullPath = version.getStoragePath();
            String fName = fullPath.substring(Math.max(fullPath.lastIndexOf("/"), fullPath.lastIndexOf("\\")) + 1);
            vDto.setFileName(fName);

            byte[] fileBytes = storageStrategy.loadTemplate(version.getStoragePath());
            vDto.setFileContentBase64(Base64.getEncoder().encodeToString(fileBytes));

            List<MigrationDto.ExportedMappingDto> mappings = new ArrayList<>();
            for (TemplateQueryMapping mapping : version.getMappings()) {
                Map<String, Object> queryDetails = connectorQueryServiceClient.getQueryDetails(mapping.getQueryId());
                Map<String, Object> connDetails = connectorQueryServiceClient
                        .getConnectorDetails(mapping.getConnectorId());

                String qName = queryDetails != null ? (String) queryDetails.get("name") : mapping.getQueryName();
                String cName = connDetails != null ? (String) connDetails.get("name") : mapping.getConnectorName();

                MigrationDto.ExportedMappingDto mDto = new MigrationDto.ExportedMappingDto();
                mDto.setQueryName(qName);
                mDto.setConnectorName(cName);
                mDto.setJsonNodeName(mapping.getJsonNodeName());
                mappings.add(mDto);

                // Collect unique queries and connectors
                if (!processedQueryIds.contains(mapping.getQueryId()) && queryDetails != null) {
                    MigrationDto.ExportedQueryDto qExport = new MigrationDto.ExportedQueryDto();
                    qExport.setName(qName);
                    qExport.setQueryText((String) queryDetails.get("queryText"));
                    qExport.setConnectorName(cName);
                    queries.add(qExport);
                    processedQueryIds.add(mapping.getQueryId());
                }

                if (!processedConnectorIds.contains(mapping.getConnectorId()) && connDetails != null) {
                    MigrationDto.ExportedConnectorDto cExport = new MigrationDto.ExportedConnectorDto();
                    cExport.setName(cName);
                    cExport.setType((String) connDetails.get("dbType"));
                    cExport.setUrl((String) connDetails.get("jdbcUrl"));
                    cExport.setUsername((String) connDetails.get("username"));
                    cExport.setHost((String) connDetails.get("host"));
                    cExport.setPort((Integer) connDetails.get("port"));
                    cExport.setDbName((String) connDetails.get("dbName"));
                    cExport.setUseRawUrl(connDetails.get("useRawUrl") != null ? (Boolean) connDetails.get("useRawUrl") : false);
                    connectors.add(cExport);
                    processedConnectorIds.add(mapping.getConnectorId());
                }
            }
            vDto.setMappings(mappings);
            versions.add(vDto);
        }

        export.setConnectors(connectors);
        export.setQueries(queries);
        export.setVersions(versions);

        return export;
    }

    @Transactional
    public ReportTemplateDto importTemplate(MigrationDto.ImportRequestDto request) {
        MigrationDto.TemplateExportDto data = request.getExportData();

        // 1. Resolve Connectors
        Map<String, String> connectorNameToId = new HashMap<>();
        Map<String, String> connectorNameToNewName = new HashMap<>();
        for (MigrationDto.ConnectorImportConfig config : request.getConnectors()) {
            MigrationDto.ExportedConnectorDto exported = data.getConnectors().stream()
                    .filter(c -> c.getName().equals(config.getOriginalName()))
                    .findFirst().orElse(null);

            if (exported == null)
                continue;

            String targetId;
            String targetName = config.getOriginalName();
            if ("SKIP".equals(config.getStrategy())) {
                Map<String, Object> existing = connectorQueryServiceClient.getConnectorByName(config.getOriginalName());
                targetId = (String) existing.get("id");
            } else if ("OVERRIDE".equals(config.getStrategy())) {
                Map<String, Object> existing = connectorQueryServiceClient.getConnectorByName(config.getOriginalName());
                targetId = (String) existing.get("id");
                connectorQueryServiceClient.updateConnector(targetId, exported, config.getOriginalName(),
                        config.getPassword());
            } else { // CREATE_NEW
                targetName = config.getTargetName();
                Map<String, Object> created = connectorQueryServiceClient.createConnector(exported,
                        targetName, config.getPassword());
                targetId = (String) created.get("id");
            }
            connectorNameToId.put(config.getOriginalName(), targetId);
            connectorNameToNewName.put(config.getOriginalName(), targetName);
        }

        // 2. Resolve Queries
        Map<String, String> queryNameToId = new HashMap<>();
        Map<String, String> queryNameToNewName = new HashMap<>();
        for (MigrationDto.QueryImportConfig config : request.getQueries()) {
            MigrationDto.ExportedQueryDto exported = data.getQueries().stream()
                    .filter(q -> q.getName().equals(config.getOriginalName()))
                    .findFirst().orElse(null);

            if (exported == null)
                continue;

            // Resolve connectorId: prefer the resolved import connector, fall back to the live record
            String connectorId = connectorNameToId.get(exported.getConnectorName());

            String targetId;
            String targetName = config.getOriginalName();

            if ("SKIP".equals(config.getStrategy())) {
                // ... same skip lookup ...
                Map<String, Object> liveQuery = null;
                if (connectorId != null) {
                    liveQuery = connectorQueryServiceClient.getQueryByName(connectorId, config.getOriginalName());
                }
                if (liveQuery == null || liveQuery.get("id") == null) {
                    // Search without connectorId — find the connector first by name
                    Map<String, Object> liveConnector = connectorQueryServiceClient.getConnectorByName(exported.getConnectorName());
                    if (liveConnector != null && liveConnector.get("id") != null) {
                        connectorId = (String) liveConnector.get("id");
                        liveQuery = connectorQueryServiceClient.getQueryByName(connectorId, config.getOriginalName());
                    }
                }
                if (liveQuery == null || liveQuery.get("id") == null) {
                    throw new RuntimeException(
                        "SKIP strategy: could not find existing query '" + config.getOriginalName() + "'. " +
                        "The connector for this query may not exist in the target environment.");
                }
                targetId = (String) liveQuery.get("id");

            } else if ("OVERRIDE".equals(config.getStrategy())) {
                // ... same override logic ...
                if (connectorId == null) {
                    Map<String, Object> liveConnector = connectorQueryServiceClient.getConnectorByName(exported.getConnectorName());
                    if (liveConnector == null || liveConnector.get("id") == null) {
                        throw new RuntimeException(
                            "OVERRIDE strategy: connector '" + exported.getConnectorName() + "' does not exist " +
                            "in the target environment. Cannot override query '" + config.getOriginalName() + "'.");
                    }
                    connectorId = (String) liveConnector.get("id");
                }
                Map<String, Object> existing = connectorQueryServiceClient.getQueryByName(connectorId, config.getOriginalName());
                if (existing == null || existing.get("id") == null) {
                    throw new RuntimeException(
                        "OVERRIDE strategy: query '" + config.getOriginalName() + "' not found on connector '" +
                        exported.getConnectorName() + "' in the target environment.");
                }
                targetId = (String) existing.get("id");
                connectorQueryServiceClient.updateQuery(targetId, exported, connectorId, config.getOriginalName());

            } else { // CREATE_NEW
                if (connectorId == null) {
                    throw new RuntimeException(
                        "Cannot create query '" + config.getTargetName() + "': connector '" +
                        exported.getConnectorName() + "' was not resolved. " +
                        "Ensure the connector is also being imported or already exists in the target.");
                }
                targetName = config.getTargetName();
                Map<String, Object> created = connectorQueryServiceClient.createQuery(exported, connectorId,
                        targetName);
                targetId = (String) created.get("id");
            }
            queryNameToId.put(config.getOriginalName(), targetId);
            queryNameToNewName.put(config.getOriginalName(), targetName);
        }

        // 3. Resolve Template
        ReportTemplate template;
        if ("SKIP".equals(request.getTemplate().getStrategy())) {
            template = templateRepository.findByName(request.getTemplate().getOriginalName())
                    .orElseThrow(() -> new RuntimeException("Template to skip not found"));
        } else if ("OVERRIDE".equals(request.getTemplate().getStrategy())) {
            template = templateRepository.findByName(request.getTemplate().getOriginalName())
                    .orElseThrow(() -> new RuntimeException("Template to override not found"));
            template.setDescription(data.getDescription());
            template = templateRepository.save(template);

            // Delete old versions
            for (ReportTemplateVersion v : template.getVersions()) {
                storageStrategy.deleteTemplate(v.getStoragePath());
                versionRepository.delete(v);
            }
            template.getVersions().clear();
        } else { // CREATE_NEW
            String targetName = request.getTemplate().getTargetName();
            // Pre-flight check: fail early BEFORE any file I/O to avoid orphaned files on disk
            if (templateRepository.findByName(targetName).isPresent()) {
                throw new RuntimeException(
                    "Cannot create template: a template named '" + targetName + "' already exists in this environment. " +
                    "Please choose a different name under 'Import as New Template'.");
            }
            template = new ReportTemplate();
            template.setName(targetName);
            template.setDescription(data.getDescription());
            template = templateRepository.save(template);
        }

        // 4. Import Versions & Mappings
        for (MigrationDto.ExportedVersionDto vExport : data.getVersions()) {
            ReportTemplateVersion version = new ReportTemplateVersion();
            version.setTemplate(template);
            version.setVersionNumber(vExport.getVersionNumber());
            version.setCreatedBy("Migration System");
            version.setIsActive(vExport.getIsActive());

            byte[] fileBytes = Base64.getDecoder().decode(vExport.getFileContentBase64());
            String newPath;
            try {
                newPath = storageStrategy.saveTemplate(template.getId() + "_v" + version.getVersionNumber(), fileBytes,
                        vExport.getFileName());
            } catch (Exception e) {
                throw new RuntimeException("Failed to save template file during migration", e);
            }
            version.setStoragePath(newPath);

            // Recreate mappings
            for (MigrationDto.ExportedMappingDto mExport : vExport.getMappings()) {
                TemplateQueryMapping mapping = new TemplateQueryMapping();
                mapping.setTemplateVersion(version);
                mapping.setJsonNodeName(mExport.getJsonNodeName());
                
                // Use new names if present, otherwise fall back to original names from JSON
                String resolvedQueryName = queryNameToNewName.getOrDefault(mExport.getQueryName(), mExport.getQueryName());
                String resolvedConnectorName = connectorNameToNewName.getOrDefault(mExport.getConnectorName(), mExport.getConnectorName());
                
                // US-7: Find the dbType for this connector in the export package
                String dbType = data.getConnectors().stream()
                    .filter(c -> c.getName().equals(mExport.getConnectorName()))
                    .map(MigrationDto.ExportedConnectorDto::getType)
                    .findFirst().orElse("H2");

                mapping.setQueryName(resolvedQueryName);
                mapping.setConnectorName(resolvedConnectorName);
                mapping.setConnectorDbType(dbType);

                mapping.setQueryId(queryNameToId.get(mExport.getQueryName()));
                mapping.setConnectorId(connectorNameToId.get(mExport.getConnectorName()));

                version.getMappings().add(mapping);
            }

            versionRepository.save(version);
        }

        return mapToDto(template);
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
        dto.setConnectorDbType(mapping.getConnectorDbType());
        dto.setJsonNodeName(mapping.getJsonNodeName());
        return dto;
    }
}
