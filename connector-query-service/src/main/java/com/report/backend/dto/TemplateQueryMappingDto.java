package com.report.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateQueryMappingDto {
    private String id;
    
    @NotNull(message = "Template ID is mandatory")
    private String templateId;
    
    @NotNull(message = "Query ID is mandatory")
    private String queryId;
    
    @NotBlank(message = "JSON Node Name is mandatory")
    private String jsonNodeName;
    
    private String queryName; // Read-only for display purposes
}
