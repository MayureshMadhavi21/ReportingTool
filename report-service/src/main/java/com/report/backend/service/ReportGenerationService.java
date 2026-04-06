package com.report.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.ReportTemplateVersionDto;
import com.report.backend.dto.TemplateQueryMappingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final ConnectorQueryServiceClient connectorQueryServiceClient;
    private final AsposeProcessingService asposeProcessingService;
    private final ReportTemplateService reportTemplateService;
    private final TemplateStorageStrategy templateStorageStrategy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generateReport(String templateId, Integer versionNumber, String outputFormat, Map<String, Object> runtimeParams) {
        ReportTemplateDto template = reportTemplateService.getTemplateById(templateId);
        
        if (template.getVersions() == null || template.getVersions().isEmpty()) {
            throw new RuntimeException("No versions found for template: " + template.getName());
        }
        
        // Find active version or default to latest if none marked active
        ReportTemplateVersionDto selectedVersion;
        if (versionNumber == null) {
            selectedVersion = template.getVersions().stream()
                    .filter(v -> Integer.valueOf(1).equals(v.getIsActive()))
                    .findFirst()
                    .orElseGet(() -> template.getVersions().stream()
                            .sorted((v1, v2) -> v2.getVersionNumber().compareTo(v1.getVersionNumber()))
                            .findFirst()
                            .get());
        } else {
            selectedVersion = template.getVersions().stream()
                    .filter(v -> v.getVersionNumber().equals(versionNumber))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Version " + versionNumber + " not found for template"));
        }
        
        List<TemplateQueryMappingDto> mappings = selectedVersion.getMappings();

        log.info("Starting generation for template: {} (Version {}) with {} queries", 
                template.getName(), selectedVersion.getVersionNumber(), mappings.size());

        // 1. Execute all mapped queries in parallel
        List<CompletableFuture<Map.Entry<String, List<Map<String, Object>>>>> futures = mappings.stream()
                .map(mapping -> {
                    String nodeName = mapping.getJsonNodeName();
                    return CompletableFuture.supplyAsync(() -> {
                        log.info("Executing query ID {} to node {} with params", mapping.getQueryId(), nodeName);
                        List<Map<String, Object>> queryResult = connectorQueryServiceClient.executeQuery(mapping.getQueryId(), runtimeParams);
                        return Map.entry(nodeName, queryResult);
                    });
                })
                .collect(Collectors.toList());

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 2. Consolidate results into a single JSON object (Map)
        Map<String, Object> consolidatedData = new HashMap<>();
        for (CompletableFuture<Map.Entry<String, List<Map<String, Object>>>> future : futures) {
            try {
                Map.Entry<String, List<Map<String, Object>>> entry = future.get();
                consolidatedData.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("Error collecting parallel query results", e);
                throw new RuntimeException("Error during parallel query execution", e);
            }
        }

        String jsonDataSource;
        try {
            jsonDataSource = objectMapper.writeValueAsString(consolidatedData);
            log.debug("GENERATED REPORT JSON:\n{}", jsonDataSource);
            log.info("Consolidated JSON generated.");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize data to JSON", e);
        }

        // 3. Pass template and JSON data to Aspose engine
        try {
            byte[] fileData = templateStorageStrategy.loadTemplate(selectedVersion.getStoragePath());
            
            // Determine file type from path if not explicitly in DTO
            String fileType = selectedVersion.getStoragePath().toLowerCase().endsWith(".xlsx") ? "XLSX" : "DOCX";
            
            return asposeProcessingService.processTemplate(fileData, jsonDataSource, fileType, outputFormat);
        } catch (Exception e) {
            log.error("Error generating report with Aspose", e);
            throw new RuntimeException("Error generating Aspose report", e);
        }
    }
}
