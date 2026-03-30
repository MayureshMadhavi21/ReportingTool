package com.report.backend.controller;

import com.report.backend.dto.ReportTemplateDto;
import com.report.backend.dto.ReportGenerationRequestDto;
import com.report.backend.service.ReportGenerationService;
import com.report.backend.service.ReportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generate")
@Tag(name = "Report Generation", description = "Trigger the generation of consolidated reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportGenerationController {

    private final ReportGenerationService generateService;
    private final ReportTemplateService templateService;

    @PostMapping
    @Operation(summary = "Generate a report using a single request body for all parameters")
    public ResponseEntity<byte[]> generateReport(@RequestBody ReportGenerationRequestDto request) {
        String templateId = request.getTemplateId();
        String format = request.getFormat() != null ? request.getFormat() : "DOCX";
        Map<String, Object> params = request.getParameters();

        byte[] reportData = generateService.generateReport(templateId, request.getVersionNumber(), format, params);
        ReportTemplateDto template = templateService.getTemplateById(templateId);

        String latestPath = template.getVersions().stream()
                .findFirst() // Versions are sorted by number in service
                .map(v -> v.getStoragePath())
                .orElse("");

        String extension = format.equalsIgnoreCase("PDF") ? ".pdf"
                : (latestPath.toLowerCase().endsWith(".xlsx") ? ".xlsx" : ".docx");

        String filename = "Generated_" + template.getName().replaceAll("\\s+", "_") + extension;

        HttpHeaders headers = new HttpHeaders();
        // Set proper content type based on output
        if ("PDF".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.APPLICATION_PDF);
        } else if ("XLSX".equalsIgnoreCase(format)) {
            headers.setContentType(
                    MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        } else {
            headers.setContentType(
                    MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        }

        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportData);
    }
}
