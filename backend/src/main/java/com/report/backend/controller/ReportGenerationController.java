package com.report.backend.controller;

import com.report.backend.entity.ReportTemplate;
import com.report.backend.service.ReportGenerationService;
import com.report.backend.service.ReportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generate")
@Tag(name = "Report Generation", description = "Trigger the generation of consolidated reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportGenerationController {

    private final ReportGenerationService generateService;
    private final ReportTemplateService templateService;

    @GetMapping("/{templateId}")
    @Operation(summary = "Generate a report for a given template")
    public ResponseEntity<byte[]> generateReport(
            @PathVariable("templateId") Long templateId,
            @RequestParam(name = "format", defaultValue = "DOCX") String format) {
        
        byte[] reportData = generateService.generateReport(templateId, format);
        ReportTemplate template = templateService.getTemplateEntityById(templateId);

        String extension = format.equalsIgnoreCase("PDF") ? ".pdf" : 
                          (template.getFileType().equalsIgnoreCase("XLSX") ? ".xlsx" : ".docx");
        
        String filename = "Generated_" + template.getName().replaceAll("\\s+", "_") + extension;

        HttpHeaders headers = new HttpHeaders();
        // Set proper content type based on output
        if ("PDF".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.APPLICATION_PDF);
        } else if ("XLSX".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        } else {
            headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        }

        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportData);
    }
}
