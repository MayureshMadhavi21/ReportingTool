package com.report.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class MigrationDto {

    @Data
    public static class TemplateExportDto {
        private String templateName;
        private String description;
        private List<ExportedConnectorDto> connectors;
        private List<ExportedQueryDto> queries;
        private List<ExportedVersionDto> versions;
    }

    @Data
    public static class ExportedConnectorDto {
        private String name;
        private String type;
        private String url;
        private String username;
        private String host;
        private Integer port;
        private String dbName;
        private boolean useRawUrl;
        private String driverClassName;
        // Password/VaultKey are EXCLUDED as per user feedback
    }

    @Data
    public static class ExportedQueryDto {
        private String name;
        private String queryText;
        private String connectorName; // Link by name for relocation
    }

    @Data
    public static class ExportedVersionDto {
        private Integer versionNumber;
        private String originalPath; // The full storage path from source
        private String fileName;     // Just the filename part for easier debugging
        private Integer isActive;
        private String fileContentBase64; // Binary content for migration
        private List<ExportedMappingDto> mappings;
    }

    @Data
    public static class ExportedMappingDto {
        private String queryName;
        private String connectorName;
        private String jsonNodeName;
    }

    @Data
    public static class ImportRequestDto {
        private TemplateExportDto exportData;
        private List<ConnectorImportConfig> connectors;
        private List<QueryImportConfig> queries;
        private TemplateImportConfig template;
    }

    @Data
    public static class ConnectorImportConfig {
        private String originalName;
        private String targetName; // Custom name for 'Create New'
        private String password;   // Manually entered password
        private String host;
        private Integer port;
        private String dbName;
        private boolean useRawUrl;
        private String strategy;   // SKIP, OVERRIDE, CREATE_NEW
    }

    @Data
    public static class QueryImportConfig {
        private String originalName;
        private String connectorName;
        private String targetName; 
        private String strategy;   // SKIP, OVERRIDE, CREATE_NEW
    }

    @Data
    public static class TemplateImportConfig {
        private String originalName;
        private String targetName;
        private String strategy;   // SKIP, OVERRIDE, CREATE_NEW
    }

    // ---- Smart Conflict Analysis DTOs ----

    @Data
    public static class MigrationAnalysisDto {
        private java.util.Map<String, ConnectorAnalysis> connectors;
        private java.util.Map<String, QueryAnalysis> queries;
        private boolean templateExists;
        // Impact maps: which template names are affected if a connector/query is overridden
        private java.util.Map<String, java.util.List<String>> connectorImpactMap;
        private java.util.Map<String, java.util.List<String>> queryImpactMap;
    }

    @Data
    public static class ConnectorAnalysis {
        private boolean exists;
        private String existingId;
    }

    @Data
    public static class QueryAnalysis {
        private boolean exists;
        private String existingId;
        private String currentQueryText; // Fetched for SQL diff view (Option B)
    }
}
