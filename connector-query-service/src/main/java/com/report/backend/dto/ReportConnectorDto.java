package com.report.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportConnectorDto {
    private String id;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "DB Type is mandatory")
    private String dbType;

    @NotBlank(message = "JDBC URL is mandatory")
    private String jdbcUrl;
    
    private String host;
    private Integer port;
    private String dbName;
    private boolean useRawUrl;

    @NotBlank(message = "Username is mandatory")
    private String username;

    @NotBlank(message = "Password is mandatory")
    private String password; // Will be encrypted on save, and not returned normally, but kept here for DTO
                             // binding.
}
