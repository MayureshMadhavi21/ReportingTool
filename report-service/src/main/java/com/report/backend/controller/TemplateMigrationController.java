package com.report.backend.controller;

import com.report.backend.dto.MigrationDto;
import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.service.ReportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/migration")
@Tag(name = "Template Migration", description = "APIs for exporting and importing templates between environments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TemplateMigrationController {

    private final ReportTemplateService service;

    @GetMapping("/export/{id}")
    @Operation(summary = "Export a template and all its dependencies as a JSON file")
    public ResponseEntity<MigrationDto.TemplateExportDto> exportTemplate(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.exportTemplate(id));
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze a migration package and detect conflicts with the current environment")
    public ResponseEntity<MigrationDto.MigrationAnalysisDto> analyzeImport(
            @RequestBody MigrationDto.TemplateExportDto exportData) {
        return ResponseEntity.ok(service.analyzeImport(exportData));
    }

    @PostMapping("/import")
    @Operation(summary = "Import a template using the provided conflict resolution strategy")
    public ResponseEntity<ReportTemplateDto> importTemplate(@RequestBody MigrationDto.ImportRequestDto request) {
        return ResponseEntity.ok(service.importTemplate(request));
    }
}
