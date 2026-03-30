package com.report.backend.repository;

import com.report.backend.entity.ReportQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportQueryRepository extends JpaRepository<ReportQuery, String> {
    List<ReportQuery> findByConnectorId(String connectorId);
}
