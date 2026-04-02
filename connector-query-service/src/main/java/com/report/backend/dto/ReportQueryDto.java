package com.report.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class ReportQueryDto {
    private String id;

    @NotNull(message = "Connector ID is mandatory")
    private String connectorId;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Query text is mandatory")
    private String queryText;

    private String connectorName;
    private String connectorDbType;

    private String description;

    private List<PlaceholderMetadataDto> placeholderMetadata;
}
