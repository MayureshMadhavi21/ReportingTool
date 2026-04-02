package com.report.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceholderMetadata {
    @Column(name = "data_type")
    private String type;
    
    @Column(name = "description", length = 500)
    private String description;
}
