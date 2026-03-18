package com.report.backend.service;

import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.repository.ReportConnectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportConnectorService {

    private final ReportConnectorRepository repository;

    public List<ReportConnectorDto> getAllConnectors() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ReportConnectorDto getConnectorById(Long id) {
        return repository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Connector not found"));
    }

    public ReportConnectorDto createConnector(ReportConnectorDto dto) {
        ReportConnector entity = new ReportConnector();
        entity.setName(dto.getName());
        entity.setDbType(dto.getDbType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setUsername(dto.getUsername());
        // In a real app, encrypt the password here
        entity.setPasswordEncrypted(dto.getPassword());
        
        return mapToDto(repository.save(entity));
    }

    public void deleteConnector(Long id) {
        repository.deleteById(id);
    }

    private ReportConnectorDto mapToDto(ReportConnector entity) {
        ReportConnectorDto dto = new ReportConnectorDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDbType(entity.getDbType());
        dto.setJdbcUrl(entity.getJdbcUrl());
        dto.setUsername(entity.getUsername());
        // Do not return password to frontend
        return dto;
    }
}
