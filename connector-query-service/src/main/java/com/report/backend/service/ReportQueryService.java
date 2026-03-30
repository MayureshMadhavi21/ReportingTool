package com.report.backend.service;

import com.report.backend.dto.ReportQueryDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportQueryService {

    private final ReportQueryRepository queryRepository;
    private final ReportConnectorRepository connectorRepository;
    private final DatabaseExecutionService databaseExecutionService;
    private final VaultService vaultService;
    private final ReportServiceClient reportServiceClient;

    @Transactional(readOnly = true)
    public List<ReportQueryDto> getAllQueries() {
        return queryRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReportQueryDto> getQueriesByConnector(String connectorId) {
        return queryRepository.findByConnectorId(connectorId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportQueryDto getQueryById(String id) {
        return queryRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Query not found"));
    }

    public ReportQueryDto createQuery(ReportQueryDto dto) {
        ReportConnector connector = connectorRepository.findById(dto.getConnectorId())
                .orElseThrow(() -> new RuntimeException("Connector not found"));

        ReportQuery entity = new ReportQuery();
        entity.setConnector(connector);
        entity.setName(dto.getName());
        entity.setQueryText(dto.getQueryText());
        entity.setDescription(dto.getDescription());
        
        return mapToDto(queryRepository.save(entity));
    }

    public ReportQueryDto updateQuery(String id, ReportQueryDto dto) {
        ReportQuery entity = queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));

        if (!entity.getConnector().getId().equals(dto.getConnectorId())) {
            ReportConnector newConnector = connectorRepository.findById(dto.getConnectorId())
                    .orElseThrow(() -> new RuntimeException("Connector not found"));
            entity.setConnector(newConnector);
        }

        entity.setName(dto.getName());
        entity.setQueryText(dto.getQueryText());
        entity.setDescription(dto.getDescription());

        return mapToDto(queryRepository.save(entity));
    }

    public void deleteQuery(String id) {
        if (reportServiceClient.isQueryMappedToTemplate(id)) {
            throw new RuntimeException("Cannot delete query because it is currently mapped to a template.");
        }
        queryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeQuery(String id, Map<String, Object> params) {
        ReportQuery query = queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        ReportConnector connector = query.getConnector();
        
        String password = vaultService.getPassword(connector.getName());
        
        return databaseExecutionService.executeQuery(
                query.getName(),
                connector.getJdbcUrl(),
                connector.getUsername(),
                password,
                query.getQueryText(),
                params
        );
    }

    @Transactional(readOnly = true)
    public Set<String> getPlaceholders(String id) {
        ReportQuery query = queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        
        Set<String> placeholders = new HashSet<>();
        Matcher matcher = Pattern.compile(":(\\w+)").matcher(query.getQueryText());
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        return placeholders;
    }

    private ReportQueryDto mapToDto(ReportQuery entity) {
        ReportQueryDto dto = new ReportQueryDto();
        dto.setId(entity.getId());
        dto.setConnectorId(entity.getConnector().getId());
        dto.setName(entity.getName());
        dto.setQueryText(entity.getQueryText());
        dto.setDescription(entity.getDescription());
        return dto;
    }
}
