package com.report.backend.repository;

import com.report.backend.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, String> {
    Optional<ReportTemplate> findByName(String name);
}
