package com.report.backend.controller;

import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.TemplateQueryMappingDto;
import com.report.backend.service.ReportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@Tag(name = "Template Management", description = "APIs for managing Word/Excel templates and mapping queries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportTemplateController {

    private final ReportTemplateService service;

    @GetMapping
    @Operation(summary = "Get all templates")
    public ResponseEntity<List<ReportTemplateDto>> getAllTemplates() {
        return ResponseEntity.ok(service.getAllTemplates());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a template by ID")
    public ResponseEntity<ReportTemplateDto> getTemplateById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getTemplateById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new template")
    public ResponseEntity<ReportTemplateDto> uploadTemplate(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.uploadTemplate(name, description, file));
    }

    @PostMapping("/{id}/mappings")
    @Operation(summary = "Add a query mapping to a template")
    public ResponseEntity<TemplateQueryMappingDto> addMapping(
            @PathVariable("id") Long id,
            @Valid @RequestBody TemplateQueryMappingDto dto) {
        return ResponseEntity.ok(service.addMapping(id, dto));
    }

    @DeleteMapping("/mappings/{mappingId}")
    @Operation(summary = "Delete a mapping")
    public ResponseEntity<Void> deleteMapping(@PathVariable("mappingId") Long mappingId) {
        service.deleteMapping(mappingId);
        return ResponseEntity.noContent().build();
    }
}
