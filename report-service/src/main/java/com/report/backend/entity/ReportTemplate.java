package com.report.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rp_report_template")
@Getter
@Setter
public class ReportTemplate {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportTemplateVersion> versions = new ArrayList<>();

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
