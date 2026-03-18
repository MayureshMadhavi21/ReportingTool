package com.report.backend.controller;

import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.service.ReportConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connectors")
@Tag(name = "Connector Management", description = "APIs for managing database connectors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportConnectorController {

    private final ReportConnectorService service;

    @GetMapping
    @Operation(summary = "Get all connectors")
    public ResponseEntity<List<ReportConnectorDto>> getAllConnectors() {
        return ResponseEntity.ok(service.getAllConnectors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a connector by ID")
    public ResponseEntity<ReportConnectorDto> getConnectorById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getConnectorById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new connector")
    public ResponseEntity<ReportConnectorDto> createConnector(@Valid @RequestBody ReportConnectorDto dto) {
        return ResponseEntity.ok(service.createConnector(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a connector")
    public ResponseEntity<Void> deleteConnector(@PathVariable("id") Long id) {
        service.deleteConnector(id);
        return ResponseEntity.noContent().build();
    }
}
