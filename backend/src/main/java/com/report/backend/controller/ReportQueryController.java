package com.report.backend.controller;

import com.report.backend.dto.ReportQueryDto;
import com.report.backend.service.ReportQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queries")
@Tag(name = "Query Management", description = "APIs for managing SQL queries mapped to connectors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportQueryController {

    private final ReportQueryService service;

    @GetMapping
    @Operation(summary = "Get all queries")
    public ResponseEntity<List<ReportQueryDto>> getAllQueries() {
        return ResponseEntity.ok(service.getAllQueries());
    }

    @GetMapping("/connector/{connectorId}")
    @Operation(summary = "Get all queries for a specific connector")
    public ResponseEntity<List<ReportQueryDto>> getQueriesByConnector(@PathVariable("connectorId") Long connectorId) {
        return ResponseEntity.ok(service.getQueriesByConnector(connectorId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a query by ID")
    public ResponseEntity<ReportQueryDto> getQueryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getQueryById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new query")
    public ResponseEntity<ReportQueryDto> createQuery(@Valid @RequestBody ReportQueryDto dto) {
        return ResponseEntity.ok(service.createQuery(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a query")
    public ResponseEntity<Void> deleteQuery(@PathVariable("id") Long id) {
        service.deleteQuery(id);
        return ResponseEntity.noContent().build();
    }
}
