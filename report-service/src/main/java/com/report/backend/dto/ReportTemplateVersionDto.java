package com.report.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReportTemplateVersionDto {
    private String id;
    private Integer versionNumber;
    private String storagePath;
    private String fileType;
    private String createdBy;
    private Integer isActive = 0;
    private LocalDateTime createdDate;
    private List<TemplateQueryMappingDto> mappings;
}
