package com.report.backend.service;

import com.report.backend.dto.ReportQueryDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportQueryService {

    private final ReportQueryRepository queryRepository;
    private final ReportConnectorRepository connectorRepository;

    public List<ReportQueryDto> getAllQueries() {
        return queryRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    public List<ReportQueryDto> getQueriesByConnector(Long connectorId) {
        return queryRepository.findByConnectorId(connectorId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ReportQueryDto getQueryById(Long id) {
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

    public void deleteQuery(Long id) {
        queryRepository.deleteById(id);
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
