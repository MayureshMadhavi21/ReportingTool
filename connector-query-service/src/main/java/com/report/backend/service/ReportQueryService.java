package com.report.backend.service;

import com.report.backend.dto.PlaceholderMetadataDto;
import com.report.backend.dto.ReportQueryDto;
import com.report.backend.entity.PlaceholderMetadata;
import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
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

    @Transactional(readOnly = true)
    public ReportQueryDto getQueryByConnectorAndName(String connectorId, String name) {
        return queryRepository.findByConnectorIdAndName(connectorId, name).map(this::mapToDto).orElse(null);
    }

    @Transactional
    public ReportQueryDto createQuery(ReportQueryDto dto) {
        if (queryRepository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("Query name '" + dto.getName() + "' already exists.");
        }

        ReportConnector connector = connectorRepository.findById(dto.getConnectorId())
                .orElseThrow(() -> new RuntimeException("Connector not found"));

        ReportQuery entity = new ReportQuery();
        entity.setConnector(connector);
        entity.setName(dto.getName());
        entity.setQueryText(dto.getQueryText());
        entity.setDescription(dto.getDescription());
        
        
        entity.getPlaceholderMetadata().clear();
        if (dto.getPlaceholderMetadata() != null) {
            log.info("Saving metadata for query {}: {} items", dto.getName(), dto.getPlaceholderMetadata().size());
            for (PlaceholderMetadataDto mDto : dto.getPlaceholderMetadata()) {
                entity.getPlaceholderMetadata().put(mDto.getName(), new PlaceholderMetadata(mDto.getType(), mDto.getDescription()));
            }
        }
        
        return mapToDto(queryRepository.save(entity));
    }

    @Transactional
    public ReportQueryDto updateQuery(String id, ReportQueryDto dto) {
        ReportQuery entity = queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));

        if (!entity.getName().equalsIgnoreCase(dto.getName())) {
            if (queryRepository.findByName(dto.getName()).isPresent()) {
                throw new RuntimeException("Another query already exists with name '" + dto.getName() + "'.");
            }
        }

        if (!entity.getConnector().getId().equals(dto.getConnectorId())) {
            ReportConnector newConnector = connectorRepository.findById(dto.getConnectorId())
                    .orElseThrow(() -> new RuntimeException("Connector not found"));
            entity.setConnector(newConnector);
        }

        entity.setName(dto.getName());
        entity.setQueryText(dto.getQueryText());
        entity.setDescription(dto.getDescription());


        // Clear and repopulate to ensure JPA handles the collection update correctly
        entity.getPlaceholderMetadata().clear();
        if (dto.getPlaceholderMetadata() != null) {
            log.debug("Updating metadata for query {}: {} items", dto.getName(), dto.getPlaceholderMetadata().size());
            for (PlaceholderMetadataDto mDto : dto.getPlaceholderMetadata()) {
                log.debug("Adding metadata: {} -> {} ({})", mDto.getName(), mDto.getType(), mDto.getDescription());
                entity.getPlaceholderMetadata().put(mDto.getName(), new PlaceholderMetadata(mDto.getType(), mDto.getDescription()));
            }
        }

        ReportQuery saved = queryRepository.save(entity);
        log.info("Saved query {} with {} placeholder metadata items", saved.getName(), saved.getPlaceholderMetadata().size());
        return mapToDto(saved);
    }

    @Transactional
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
                connector.getDbType(),
                connector.getJdbcUrl(),
                connector.getUsername(),
                password,
                query.getQueryText(),
                params,
                query.getPlaceholderMetadata()
        );
    }

    @Transactional(readOnly = true)
    public void validateQuerySyntax(ReportQueryDto dto) {
        ReportConnector connector = connectorRepository.findById(dto.getConnectorId())
                .orElseThrow(() -> new RuntimeException("Connector not found"));
        
        String password = vaultService.getPassword(connector.getName());
        
        databaseExecutionService.validateQuery(
                connector.getDbType(),
                connector.getJdbcUrl(),
                connector.getUsername(),
                password,
                dto.getQueryText()
        );
    }

    @Transactional(readOnly = true)
    public List<PlaceholderMetadataDto> getPlaceholders(String id) {
        ReportQuery query = queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));
        
        Set<String> foundNames = new HashSet<>();
        Matcher matcher = Pattern.compile(":(\\w+)").matcher(query.getQueryText());
        while (matcher.find()) {
            foundNames.add(matcher.group(1));
        }

        Map<String, PlaceholderMetadata> existing = query.getPlaceholderMetadata();
        return foundNames.stream().map(name -> {
            PlaceholderMetadata meta = existing.get(name);
            return new PlaceholderMetadataDto(
                    name,
                    meta != null ? meta.getType() : "STRING",
                    meta != null ? meta.getDescription() : ""
            );
        }).collect(Collectors.toList());
    }

    private ReportQueryDto mapToDto(ReportQuery entity) {
        ReportQueryDto dto = new ReportQueryDto();
        dto.setId(entity.getId());
        dto.setConnectorId(entity.getConnector().getId());
        dto.setConnectorName(entity.getConnector().getName());
        dto.setConnectorDbType(entity.getConnector().getDbType());
        dto.setName(entity.getName());
        dto.setQueryText(entity.getQueryText());
        dto.setDescription(entity.getDescription());
        
        List<PlaceholderMetadataDto> metadataList = new ArrayList<>();
        if (entity.getPlaceholderMetadata() != null) {
            for (Map.Entry<String, PlaceholderMetadata> entry : entity.getPlaceholderMetadata().entrySet()) {
                metadataList.add(new PlaceholderMetadataDto(
                        entry.getKey(),
                        entry.getValue().getType(),
                        entry.getValue().getDescription()
                ));
            }
        }
        dto.setPlaceholderMetadata(metadataList);
        return dto;
    }
}
