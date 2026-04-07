package com.report.backend.service;

import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportConnectorServiceTest {

    @Mock
    private ReportConnectorRepository repository;

    @Mock
    private ReportQueryRepository queryRepository;

    @Mock
    private VaultService vaultService;

    @InjectMocks
    private ReportConnectorService service;

    private ReportConnector connector;
    private ReportConnectorDto dto;
    private static final String CONNECTOR_ID = "connector-uuid-001";

    @BeforeEach
    void setUp() {
        connector = new ReportConnector();
        connector.setId(CONNECTOR_ID);
        connector.setName("OldName");
        connector.setDbType("H2");
        connector.setJdbcUrl("jdbc:h2:mem:testdb");
        connector.setUsername("sa");

        dto = new ReportConnectorDto();
        dto.setId(CONNECTOR_ID);
        dto.setName("NewName");
        dto.setDbType("H2");
        dto.setJdbcUrl("jdbc:h2:mem:testdb");
        dto.setUsername("sa");
        dto.setPassword("password");
    }

    @Test
    void testConnection_Success() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection conn = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);

            assertDoesNotThrow(() -> service.testConnection(dto));
        }
    }

    @Test
    void testConnection_Failure_InvalidUrl() throws SQLException {
        SQLException sqlException = new SQLException("Invalid URL");
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenThrow(sqlException);

            assertThrows(RuntimeException.class, () -> service.testConnection(dto));
        }
    }

    @Test
    void testUpdateConnector_Success_WithNameChange() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection conn = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);

            when(repository.findById(CONNECTOR_ID)).thenReturn(Optional.of(connector));
            when(repository.save(any(ReportConnector.class))).thenAnswer(i -> i.getArguments()[0]);
            when(vaultService.getPassword("OldName")).thenReturn("password");

            dto.setPassword(null); // Simulate only changing name
            
            ReportConnectorDto updated = service.updateConnector(CONNECTOR_ID, dto);
            
            assertEquals("NewName", updated.getName());
            verify(vaultService).storePassword(eq("NewName"), eq("password"));
            verify(vaultService).deletePassword("OldName");
        }
    }

    @Test
    void testUpdateConnector_Success_WithNewPassword() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection conn = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);

            when(repository.findById(CONNECTOR_ID)).thenReturn(Optional.of(connector));
            when(repository.save(any(ReportConnector.class))).thenAnswer(i -> i.getArguments()[0]);

            dto.setName("OldName");
            dto.setPassword("new-secret");
            
            service.updateConnector(CONNECTOR_ID, dto);
            
            verify(vaultService).storePassword("OldName", "new-secret");
        }
    }

    @Test
    void testCreateConnector_Success() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection conn = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);

            when(repository.save(any())).thenAnswer(i -> {
                ReportConnector saved = i.getArgument(0);
                saved.setId("new-connector-uuid");
                return saved;
            });

            ReportConnectorDto result = service.createConnector(dto);
            
            assertNotNull(result);
            assertEquals("new-connector-uuid", result.getId());
            verify(vaultService).storePassword(eq("NewName"), eq("password"));
        }
    }

    @Test
    void testDeleteConnector_Failure_InUse() {
        // Simulate queries using this connector
        when(repository.findById(CONNECTOR_ID)).thenReturn(Optional.of(connector));
        when(queryRepository.findByConnectorId(CONNECTOR_ID)).thenReturn(Collections.singletonList(new com.report.backend.entity.ReportQuery()));

        Exception exception = assertThrows(RuntimeException.class, () -> service.deleteConnector(CONNECTOR_ID));
        assertTrue(exception.getMessage().contains("used by one or more queries"));
    }
}
