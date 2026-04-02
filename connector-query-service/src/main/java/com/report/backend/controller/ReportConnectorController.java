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
import java.util.Map;

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
    public ResponseEntity<ReportConnectorDto> getConnectorById(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.getConnectorById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Find a connector by name")
    public ResponseEntity<ReportConnectorDto> getConnectorByName(@RequestParam("name") String name) {
        ReportConnectorDto dto = service.getConnectorByName(name);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Register a new connector")
    public ResponseEntity<ReportConnectorDto> createConnector(@Valid @RequestBody ReportConnectorDto dto) {
        return ResponseEntity.ok(service.createConnector(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update all connector fields")
    public ResponseEntity<ReportConnectorDto> updateConnector(@PathVariable("id") String id, @Valid @RequestBody ReportConnectorDto dto) {
        return ResponseEntity.ok(service.updateConnector(id, dto));
    }

    @PostMapping("/test")
    @Operation(summary = "Test database connectivity")
    public ResponseEntity<Map<String, String>> testConnection(@RequestBody ReportConnectorDto dto) {
        service.testConnection(dto);
        return ResponseEntity.ok(Map.of("message", "Connection successful"));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Update connector password")
    public ResponseEntity<ReportConnectorDto> updateConnectorPassword(
            @PathVariable("id") String id,
            @RequestBody Map<String, String> payload) {
        String newPassword = payload.get("password");
        return ResponseEntity.ok(service.updateConnectorPassword(id, newPassword));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a connector")
    public ResponseEntity<Void> deleteConnector(@PathVariable("id") String id) {
        service.deleteConnector(id);
        return ResponseEntity.noContent().build();
    }
}
