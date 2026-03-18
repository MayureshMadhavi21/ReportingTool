package com.report.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.backend.entity.ReportTemplate;
import com.report.backend.entity.TemplateQueryMapping;
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

    private final DatabaseExecutionService databaseExecutionService;
    private final AsposeProcessingService asposeProcessingService;
    private final ReportTemplateService reportTemplateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] generateReport(Long templateId, String outputFormat) {
        ReportTemplate template = reportTemplateService.getTemplateEntityById(templateId);
        List<TemplateQueryMapping> mappings = template.getMappings();

        log.info("Starting generation for template: {} with {} queries", template.getName(), mappings.size());

        // 1. Execute all mapped queries in parallel
        // Important: We must extract proxy fields sequentially here so Hibernate maintains its Session Context.
        List<CompletableFuture<Map.Entry<String, List<Map<String, Object>>>>> futures = mappings.stream()
                .map(mapping -> {
                    String nodeName = mapping.getJsonNodeName();
                    String queryName = mapping.getQuery().getName();
                    String queryText = mapping.getQuery().getQueryText();
                    String jdbcUrl = mapping.getQuery().getConnector().getJdbcUrl();
                    String username = mapping.getQuery().getConnector().getUsername();
                    String password = mapping.getQuery().getConnector().getPasswordEncrypted();
                    
                    return CompletableFuture.supplyAsync(() -> {
                        log.info("Executing query {} to node {}", queryName, nodeName);
                        List<Map<String, Object>> queryResult = databaseExecutionService.executeQuery(
                                queryName, jdbcUrl, username, password, queryText
                        );
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
                // Aspose JSON engine usually likes a list if you are iterating, or an object if it's 1 row.
                // We'll wrap lists inside the node name.
                consolidatedData.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("Error collecting parallel query results", e);
                throw new RuntimeException("Error during parallel query execution", e);
            }
        }

        String jsonDataSource;
        try {
            jsonDataSource = objectMapper.writeValueAsString(consolidatedData);
            log.debug("Consolidated JSON: {}", jsonDataSource);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize data to JSON", e);
        }

        // 3. Pass template and JSON data to Aspose engine
        try {
            return asposeProcessingService.processTemplate(template.getFileData(), jsonDataSource, outputFormat);
        } catch (Exception e) {
            log.error("Error generating report with Aspose", e);
            throw new RuntimeException("Error generating Aspose report", e);
        }
    }
}
