package com.report.backend.service;

import com.report.backend.dto.MigrationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConnectorQueryServiceClient {

    private final RestClient restClient;

    public ConnectorQueryServiceClient(
            @Value("${connector-query-service.url:http://localhost:8085}") String serviceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .build();
    }

    public boolean validateQueryExists(String queryId) {
        try {
            restClient.get()
                    .uri("/api/queries/{id}", queryId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Query Validation Failed for ID {}: {}", queryId, e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getQueryDetails(String queryId) {
        try {
            return restClient.get()
                    .uri("/api/queries/{id}", queryId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            log.error("Failed to fetch query details for {}: {}", queryId, e.getMessage());
            return null;
        }
    }

    public String getQueryName(String queryId) {
        Map<String, Object> details = getQueryDetails(queryId);
        return details != null ? (String) details.get("name") : "Unknown Query";
    }

    public List<Map<String, Object>> executeQuery(String queryId, Map<String, Object> params) {
        log.info("Executing query {} with params via connector-query-service", queryId);
        return restClient.post()
                .uri("/api/queries/{id}/execute", queryId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(params != null ? params : Map.of())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                });
    }

    public List<com.report.backend.dto.PlaceholderMetadataDto> getPlaceholders(String queryId) {
        return restClient.get()
                .uri("/api/queries/{id}/placeholders", queryId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<com.report.backend.dto.PlaceholderMetadataDto>>() {
                });
    }

    // Migration Methods
    public Map<String, Object> getConnectorByName(String name) {
        try {
            return restClient.get()
                    .uri("/api/connectors/search?name={name}", name)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getConnectorDetails(String connectorId) {
        try {
            return restClient.get()
                    .uri("/api/connectors/{id}", connectorId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            log.error("Failed to fetch connector details for {}: {}", connectorId, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getQueryByName(String connectorId, String name) {
        try {
            return restClient.get()
                    .uri("/api/queries/search?connectorId={connectorId}&name={name}", connectorId, name)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> createConnector(MigrationDto.ExportedConnectorDto dto, String name, String password) {
        Map<String, Object> body = Map.of(
                "name", name,
                "dbType", dto.getType(),
                "jdbcUrl", dto.getUrl(),
                "host", dto.getHost() != null ? dto.getHost() : "",
                "port", dto.getPort(),
                "dbName", dto.getDbName() != null ? dto.getDbName() : "",
                "useRawUrl", dto.isUseRawUrl(),
                "username", dto.getUsername(),
                "password", password);
        return restClient.post()
                .uri("/api/connectors")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }

    public Map<String, Object> updateConnector(String id, MigrationDto.ExportedConnectorDto dto, String name,
            String password) {
        Map<String, Object> body = Map.of(
                "name", name,
                "dbType", dto.getType(),
                "jdbcUrl", dto.getUrl(),
                "host", dto.getHost() != null ? dto.getHost() : "",
                "port", dto.getPort(),
                "dbName", dto.getDbName() != null ? dto.getDbName() : "",
                "useRawUrl", dto.isUseRawUrl(),
                "username", dto.getUsername(),
                "password", password != null ? password : "");
        return restClient.put()
                .uri("/api/connectors/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }

    public Map<String, Object> createQuery(MigrationDto.ExportedQueryDto dto, String connectorId, String name) {
        Map<String, Object> body = Map.of(
                "connectorId", connectorId,
                "name", name,
                "queryText", dto.getQueryText(),
                "description", "Imported via migration");
        return restClient.post()
                .uri("/api/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }

    public Map<String, Object> updateQuery(String id, MigrationDto.ExportedQueryDto dto, String connectorId,
            String name) {
        Map<String, Object> body = Map.of(
                "connectorId", connectorId,
                "name", name,
                "queryText", dto.getQueryText(),
                "description", "Updated via migration");
        return restClient.put()
                .uri("/api/queries/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }
}
