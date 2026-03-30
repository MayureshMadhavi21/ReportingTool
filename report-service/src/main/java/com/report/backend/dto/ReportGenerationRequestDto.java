package com.report.backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class ReportGenerationRequestDto {
    private String templateId;
    private Integer versionNumber; // Optional, defaults to latest
    private String format; // Default to DOCX if not provided
    private Map<String, Object> parameters;
}
