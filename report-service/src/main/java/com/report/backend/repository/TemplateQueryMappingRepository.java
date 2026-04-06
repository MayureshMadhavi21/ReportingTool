package com.report.backend.repository;

import com.report.backend.entity.TemplateQueryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateQueryMappingRepository extends JpaRepository<TemplateQueryMapping, String> {
    List<TemplateQueryMapping> findByTemplateVersionId(String templateVersionId);
    boolean existsByQueryId(String queryId);
    List<TemplateQueryMapping> findByQueryId(String queryId);
}
