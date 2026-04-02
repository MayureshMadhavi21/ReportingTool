package com.report.backend.service;

import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportConnectorService {

    private final ReportConnectorRepository repository;
    private final ReportQueryRepository queryRepository;
    private final VaultService vaultService;

    @Transactional(readOnly = true)
    public List<ReportConnectorDto> getAllConnectors() {
        return repository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportConnectorDto getConnectorById(String id) {
        return repository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Connector not found"));
    }

    @Transactional(readOnly = true)
    public ReportConnectorDto getConnectorByName(String name) {
        return repository.findByName(name).map(this::mapToDto).orElse(null);
    }

    @Transactional
    public ReportConnectorDto createConnector(ReportConnectorDto dto) {
        // Step 1: Connectivity check before creating
        testConnection(dto);

        ReportConnector entity = new ReportConnector();
        entity.setName(dto.getName());
        entity.setDbType(dto.getDbType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setDbName(dto.getDbName());
        entity.setUseRawUrl(dto.isUseRawUrl());
        entity.setUsername(dto.getUsername());
        entity.setPasswordEncrypted("VAULT_RESTRICTED"); // Placeholder
        
        entity = repository.save(entity);
        vaultService.storePassword(entity.getName(), dto.getPassword());
        
        return mapToDto(entity);
    }

    public void testConnection(ReportConnectorDto dto) {
        String password = dto.getPassword();
        // If password is blank and it's an update, fetch from vault
        if ((password == null || password.isEmpty()) && dto.getId() != null) {
            ReportConnector entity = repository.findById(dto.getId()).orElse(null);
            if (entity != null) password = vaultService.getPassword(entity.getName());
        }

        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Password is required for connection test");
        }

        loadDriverHelper(dto.getDbType());

        try (Connection conn = DriverManager.getConnection(dto.getJdbcUrl(), dto.getUsername(), password)) {
            // Success
        } catch (SQLException e) {
            throw new RuntimeException("Connection failed: " + e.getMessage());
        }
    }

    private void loadDriverHelper(String dbType) {
        if (dbType == null) return;
        String driverClass = switch (dbType.toUpperCase()) {
            case "SQL_SERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "MYSQL" -> "com.mysql.cj.jdbc.Driver";
            case "POSTGRESQL" -> "org.postgresql.Driver";
            case "ORACLE" -> "oracle.jdbc.OracleDriver";
            case "H2" -> "org.h2.Driver";
            default -> null;
        };

        if (driverClass != null) {
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                if ("ORACLE".equals(dbType.toUpperCase()) || "MYSQL".equals(dbType.toUpperCase()) || "POSTGRESQL".equals(dbType.toUpperCase())) {
                    throw new RuntimeException("JDBC Driver for " + dbType + " not found. Please ensure it is in pom.xml.");
                }
            }
        }
    }

    @Transactional
    public ReportConnectorDto updateConnector(String id, ReportConnectorDto dto) {
        ReportConnector entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connector not found"));
        
        // Step 1: Connectivity check before updating
        testConnection(dto);

        String oldName = entity.getName();
        String newName = dto.getName();

        entity.setName(dto.getName());
        entity.setDbType(dto.getDbType());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setHost(dto.getHost());
        entity.setPort(dto.getPort());
        entity.setDbName(dto.getDbName());
        entity.setUseRawUrl(dto.isUseRawUrl());
        entity.setUsername(dto.getUsername());

        // Update vault
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            vaultService.storePassword(newName, dto.getPassword());
            if (!oldName.equals(newName)) vaultService.deletePassword(oldName);
        } else if (!oldName.equals(newName)) {
            // Name changed but password stayed same, migrate vault key
            String existingPass = vaultService.getPassword(oldName);
            vaultService.storePassword(newName, existingPass);
            vaultService.deletePassword(oldName);
        }

        entity = repository.save(entity);
        return mapToDto(entity);
    }

    @Transactional
    public ReportConnectorDto updateConnectorPassword(String id, String newPassword) {
        ReportConnector entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connector not found"));
        
        // Connectivity check with new password
        ReportConnectorDto testDto = mapToDto(entity);
        testDto.setPassword(newPassword);
        testConnection(testDto);

        vaultService.storePassword(entity.getName(), newPassword);
        return mapToDto(entity);
    }

    @Transactional
    public void deleteConnector(String id) {
        ReportConnector entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connector not found"));

        if (!queryRepository.findByConnectorId(id).isEmpty()) {
            throw new RuntimeException("Cannot delete connector because it is used by one or more queries.");
        }

        vaultService.deletePassword(entity.getName());
        repository.deleteById(id);
    }

    private ReportConnectorDto mapToDto(ReportConnector entity) {
        ReportConnectorDto dto = new ReportConnectorDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDbType(entity.getDbType());
        dto.setJdbcUrl(entity.getJdbcUrl());
        dto.setHost(entity.getHost());
        dto.setPort(entity.getPort());
        dto.setDbName(entity.getDbName());
        dto.setUseRawUrl(entity.isUseRawUrl());
        dto.setUsername(entity.getUsername());
        // Do not return password to frontend
        return dto;
    }
}
