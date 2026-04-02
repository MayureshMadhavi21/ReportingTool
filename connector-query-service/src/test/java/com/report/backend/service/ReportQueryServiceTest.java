package com.report.backend.service;

import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.report.backend.dto.PlaceholderMetadataDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    @Mock
    private ReportQueryRepository queryRepository;

    @Mock
    private ReportConnectorRepository connectorRepository;

    @Mock
    private DatabaseExecutionService databaseExecutionService;

    @Mock
    private VaultService vaultService;

    @Mock
    private ReportServiceClient reportServiceClient;

    @InjectMocks
    private ReportQueryService service;

    private ReportQuery query;

    @BeforeEach
    void setUp() {
        ReportConnector connector = new ReportConnector();
        connector.setId("10");
        connector.setName("TestConn");
        connector.setJdbcUrl("jdbc:h2:mem:test");
        connector.setUsername("sa");

        query = new ReportQuery();
        query.setId("1");
        query.setConnector(connector);
        query.setName("GetEmployee");
        query.setQueryText("SELECT * FROM employees WHERE dept = :dept AND active = :status");
    }

    @Test
    void testGetPlaceholders_FindsAll() {
        when(queryRepository.findById("1")).thenReturn(Optional.of(query));

        List<PlaceholderMetadataDto> placeholders = service.getPlaceholders("1");

        assertEquals(2, placeholders.size());
        assertTrue(placeholders.stream().anyMatch(p -> p.getName().equals("dept")));
        assertTrue(placeholders.stream().anyMatch(p -> p.getName().equals("status")));
    }

    @Test
    void testExecuteQuery_PassesParams() {
        when(queryRepository.findById("1")).thenReturn(Optional.of(query));
        when(vaultService.getPassword("TestConn")).thenReturn("secret");
        
        Map<String, Object> params = Map.of("dept", "IT", "status", true);
        
        service.executeQuery("1", params);
        
        verify(databaseExecutionService).executeQuery(
                eq("GetEmployee"),
                any(), // dbType
                eq("jdbc:h2:mem:test"),
                eq("sa"),
                eq("secret"),
                eq(query.getQueryText()),
                eq(params),
                anyMap() // placeholderTypes
        );
    }
}
