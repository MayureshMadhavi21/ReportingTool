package com.report.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "rp_report_connector")
@Getter
@Setter
public class ReportConnector {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "db_type", nullable = false, length = 50)
    private String dbType; // SQL_SERVER, MYSQL, POSTGRESQL, ORACLE

    @Column(name = "jdbc_url", nullable = false, length = 500)
    private String jdbcUrl;

    @Column(length = 255)
    private String host;

    @Column
    private Integer port;

    @Column(name = "db_name", length = 100)
    private String dbName;

    @Column(name = "use_raw_url")
    private boolean useRawUrl;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "password_encrypted")
    private String passwordEncrypted;

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
