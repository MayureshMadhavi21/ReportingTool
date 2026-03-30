package com.report.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportQueryDto {
    private String id;

    @NotNull(message = "Connector ID is mandatory")
    private String connectorId;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Query text is mandatory")
    private String queryText;

    private String description;
}
