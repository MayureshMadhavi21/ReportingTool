package com.report.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReportTemplateDto {
    private String id;
    private String name;
    private String description;
    private Integer latestVersionNumber;
    private List<ReportTemplateVersionDto> versions;
}
