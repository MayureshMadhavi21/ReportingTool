package com.report.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rp_report_template_version")
@Getter
@Setter
public class ReportTemplateVersion {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "storage_path", length = 1000, nullable = false)
    private String storagePath;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "is_active", nullable = false)
    private Integer isActive = 0; // 0 = Inactive, 1 = Active

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateQueryMapping> mappings = new ArrayList<>();

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
