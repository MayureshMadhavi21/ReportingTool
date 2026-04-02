package com.report.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "Report_Connector")
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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
