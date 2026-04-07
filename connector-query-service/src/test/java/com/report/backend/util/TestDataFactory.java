package com.report.backend.util;

import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.dto.ReportQueryDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;

import java.util.ArrayList;

public class TestDataFactory {

    public static ReportConnectorDto createConnectorDto() {
        ReportConnectorDto dto = new ReportConnectorDto();
        dto.setId("conn-123");
        dto.setName("Test Connector");
        dto.setDbType("H2");
        dto.setJdbcUrl("jdbc:h2:mem:testdb");
        dto.setUsername("sa");
        dto.setPassword("password");
        return dto;
    }

    public static ReportConnector createConnectorEntity() {
        ReportConnector entity = new ReportConnector();
        entity.setId("conn-123");
        entity.setName("Test Connector");
        entity.setDbType("H2");
        entity.setJdbcUrl("jdbc:h2:mem:testdb");
        entity.setUsername("sa");
        return entity;
    }

    public static ReportQueryDto createQueryDto() {
        ReportQueryDto dto = new ReportQueryDto();
        dto.setId("query-123");
        dto.setConnectorId("conn-123");
        dto.setName("Test Query");
        dto.setQueryText("SELECT * FROM test");
        dto.setPlaceholderMetadata(new ArrayList<>());
        return dto;
    }

    public static ReportQuery createQueryEntity() {
        ReportQuery entity = new ReportQuery();
        entity.setId("query-123");
        entity.setConnector(createConnectorEntity());
        entity.setName("Test Query");
        entity.setQueryText("SELECT * FROM test");
        return entity;
    }
}
