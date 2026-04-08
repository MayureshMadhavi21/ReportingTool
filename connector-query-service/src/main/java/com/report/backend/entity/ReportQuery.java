package com.report.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "rp_report_query")
@Getter
@Setter
public class ReportQuery {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_id", nullable = false)
    private ReportConnector connector;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Lob
    @Column(name = "query_text", nullable = false)
    private String queryText;

    @Column(length = 500)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rp_report_query_placeholder_metadata", joinColumns = @JoinColumn(name = "query_id"))
    @MapKeyColumn(name = "placeholder_name")
    private java.util.Map<String, PlaceholderMetadata> placeholderMetadata = new java.util.HashMap<>();

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        this.createdDate = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
        this.createdBy = "system";
        this.modifiedBy = "system";
        this.isDeleted = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedDate = LocalDateTime.now();
        this.modifiedBy = "system";
    }
}
