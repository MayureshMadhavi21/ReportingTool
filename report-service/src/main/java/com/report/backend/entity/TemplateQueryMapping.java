package com.report.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "rp_template_query_mapping", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "template_version_id", "json_node_name" })
})
@Getter
@Setter
public class TemplateQueryMapping {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private ReportTemplateVersion templateVersion;

    @Column(name = "query_id", nullable = false, length = 36)
    private String queryId;

    @Column(name = "query_name")
    private String queryName;

    @Column(name = "connector_id", length = 36)
    private String connectorId;

    @Column(name = "connector_name")
    private String connectorName;

    @Column(name = "connector_db_type")
    private String connectorDbType;

    @Column(name = "json_node_name", nullable = false, length = 100)
    private String jsonNodeName;

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
