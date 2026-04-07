package com.report.backend.util;

import com.report.backend.dto.*;
import com.report.backend.entity.ReportTemplate;
import com.report.backend.entity.ReportTemplateVersion;

import java.util.ArrayList;
import java.util.List;

public class TestDataFactory {

    public static ReportTemplateDto createTemplateDto() {
        ReportTemplateDto dto = new ReportTemplateDto();
        dto.setId("temp-123");
        dto.setName("Test Template");
        dto.setVersions(new ArrayList<>());
        return dto;
    }

    public static ReportTemplate createTemplateEntity() {
        ReportTemplate entity = new ReportTemplate();
        entity.setId("temp-123");
        entity.setName("Test Template");
        return entity;
    }

    public static ReportTemplateVersionDto createVersionDto() {
        ReportTemplateVersionDto dto = new ReportTemplateVersionDto();
        dto.setId("ver-1");
        dto.setVersionNumber(1);
        dto.setStoragePath("/data/templates/test.docx");
        dto.setIsActive(1);
        dto.setMappings(new ArrayList<>());
        return dto;
    }

    public static ReportGenerationRequestDto createGenerationRequest() {
        ReportGenerationRequestDto request = new ReportGenerationRequestDto();
        request.setTemplateId("temp-123");
        request.setVersionNumber(1);
        request.setFormat("PDF");
        request.setParameters(new java.util.HashMap<>());
        return request;
    }

    public static TemplateQueryMappingDto createMappingDto() {
        TemplateQueryMappingDto dto = new TemplateQueryMappingDto();
        dto.setQueryId("query-1");
        dto.setJsonNodeName("dataNode");
        return dto;
    }
}
