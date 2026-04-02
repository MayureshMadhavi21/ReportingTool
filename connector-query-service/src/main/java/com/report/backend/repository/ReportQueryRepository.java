package com.report.backend.repository;

import com.report.backend.entity.ReportQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportQueryRepository extends JpaRepository<ReportQuery, String> {
    List<ReportQuery> findByConnectorId(String connectorId);
    Optional<ReportQuery> findByConnectorIdAndName(String connectorId, String name);
    Optional<ReportQuery> findByName(String name);
}
