package com.report.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceholderMetadataDto {
    private String name;
    private String type;
    private String description;
}
