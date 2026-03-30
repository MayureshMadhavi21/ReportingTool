package com.report.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ConnectorQueryServiceClient {

    private final RestClient restClient;

    public ConnectorQueryServiceClient(@Value("${connector-query-service.url:http://localhost:8085}") String serviceUrl) {
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
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
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
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public Set<String> getPlaceholders(String queryId) {
        return restClient.get()
                .uri("/api/queries/{id}/placeholders", queryId)
                .retrieve()
                .body(new ParameterizedTypeReference<Set<String>>() {});
    }
}
