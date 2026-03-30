package com.report.backend.repository;

import com.report.backend.entity.ReportTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateVersionRepository extends JpaRepository<ReportTemplateVersion, String> {
    Optional<ReportTemplateVersion> findTopByTemplateIdOrderByVersionNumberDesc(String templateId);
    List<ReportTemplateVersion> findByTemplateId(String templateId);
    Optional<ReportTemplateVersion> findByTemplateIdAndVersionNumber(String templateId, Integer versionNumber);
}
