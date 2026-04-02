package com.report.backend.repository;

import com.report.backend.entity.ReportConnector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReportConnectorRepository extends JpaRepository<ReportConnector, String> {
    Optional<ReportConnector> findByName(String name);
}
