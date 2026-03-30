package com.report.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ReportServiceClient {

    private final RestClient restClient;

    public ReportServiceClient(@Value("${report-service.url:http://localhost:8084}") String serviceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .build();
    }

    public boolean isQueryMappedToTemplate(String queryId) {
        try {
            Boolean isMapped = restClient.get()
                    .uri("/api/templates/mapping-check?queryId={id}", queryId)
                    .retrieve()
                    .body(Boolean.class);
            return isMapped != null && isMapped;
        } catch (Exception e) {
            log.warn("Mapping check failed for query ID {}: {}", queryId, e.getMessage());
            // Fail closed to prevent accidental deletions
            return true; 
        }
    }
}
