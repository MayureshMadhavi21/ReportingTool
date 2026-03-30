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
import java.util.Set;

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
    public ResponseEntity<ReportTemplateDto> getTemplate(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.getTemplateById(id));
    }

    @GetMapping("/{id}/placeholders")
    @Operation(summary = "Get all combined placeholders for all queries in a template (latest version)")
    public ResponseEntity<Set<String>> getPlaceholders(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.getPlaceholdersForTemplate(id));
    }

    @GetMapping("/versions/{versionId}/placeholders")
    @Operation(summary = "Get all combined placeholders for a specific version")
    public ResponseEntity<Set<String>> getVersionPlaceholders(@PathVariable("versionId") String versionId) {
        return ResponseEntity.ok(service.getPlaceholdersForVersion(versionId));
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
    @Operation(summary = "Add a query mapping to a template (latest version)")
    public ResponseEntity<TemplateQueryMappingDto> addMapping(
            @PathVariable("id") String id,
            @Valid @RequestBody TemplateQueryMappingDto dto) {
        return ResponseEntity.ok(service.addMapping(id, dto));
    }

    @PostMapping("/versions/{versionId}/mappings")
    @Operation(summary = "Add a query mapping to a specific template version")
    public ResponseEntity<TemplateQueryMappingDto> addMappingToVersion(
            @PathVariable("versionId") String versionId,
            @Valid @RequestBody TemplateQueryMappingDto dto) {
        return ResponseEntity.ok(service.addMappingToVersion(versionId, dto));
    }

    @PostMapping("/versions/{versionId}/activate")
    @Operation(summary = "Mark a specific version as the active/latest version (Rollback)")
    public ResponseEntity<Void> activateVersion(@PathVariable("versionId") String versionId) {
        service.activateVersion(versionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/versions/{versionId}")
    @Operation(summary = "Delete a specific template version")
    public ResponseEntity<Void> deleteVersion(@PathVariable("versionId") String versionId) {
        service.deleteVersion(versionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/mappings/{mappingId}")
    @Operation(summary = "Delete a mapping")
    public ResponseEntity<Void> deleteMapping(@PathVariable("mappingId") String mappingId) {
        service.deleteMapping(mappingId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update template file")
    public ResponseEntity<ReportTemplateDto> updateTemplateFile(
            @PathVariable("id") String id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.updateTemplateFile(id, file));
    }

    @PutMapping("/mappings/{mappingId}")
    @Operation(summary = "Update an existing mapping")
    public ResponseEntity<TemplateQueryMappingDto> updateMapping(
            @PathVariable("mappingId") String mappingId,
            @Valid @RequestBody TemplateQueryMappingDto dto) {
        return ResponseEntity.ok(service.updateMapping(mappingId, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") String id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template name and description")
    public ResponseEntity<ReportTemplateDto> updateTemplateInfo(
            @PathVariable("id") String id,
            @RequestBody @Valid ReportTemplateDto dto) {
        return ResponseEntity.ok(service.updateTemplateInfo(id, dto.getName(), dto.getDescription()));
    }

    @GetMapping("/mapping-check")
    @Operation(summary = "Check if a query is used in any templates")
    public ResponseEntity<Boolean> isQueryMappedToTemplate(@RequestParam("queryId") String queryId) {
        return ResponseEntity.ok(service.isQueryMapped(queryId));
    }

    @GetMapping("/versions/{versionId}/file")
    @Operation(summary = "Download template file for a specific version")
    public ResponseEntity<byte[]> getTemplateFile(@PathVariable("versionId") String versionId) {
        byte[] data = service.getTemplateFile(versionId);
        String filename = service.getTemplateFilename(versionId);

        String contentType = filename.toLowerCase().endsWith(".xlsx") 
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
