package com.report.backend.service;

import com.report.backend.dto.ReportConnectorDto;
import com.report.backend.entity.ReportConnector;
import com.report.backend.repository.ReportConnectorRepository;
import com.report.backend.repository.ReportQueryRepository;
import com.report.backend.util.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportConnectorServiceTest {

    @Mock
    private ReportConnectorRepository connectorRepository;

    @Mock
    private ReportQueryRepository queryRepository;

    @Mock
    private VaultService vaultService;

    @Mock
    private DatabaseExecutionService databaseExecutionService;

    @InjectMocks
    private ReportConnectorService connectorService;

    @Test
    void getAllConnectors_ShouldReturnDtoList() {
        ReportConnector entity = TestDataFactory.createConnectorEntity();
        when(connectorRepository.findAll()).thenReturn(Collections.singletonList(entity));

        List<ReportConnectorDto> results = connectorService.getAllConnectors();

        assertEquals(1, results.size());
        assertEquals(entity.getName(), results.get(0).getName());
    }

    @Test
    void createConnector_ShouldEncryptPasswordAndSave() throws SQLException {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        dto.setPassword("new-password");
        ReportConnector entity = TestDataFactory.createConnectorEntity();
        
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConn);
            
            when(connectorRepository.save(any(ReportConnector.class))).thenReturn(entity);

            ReportConnectorDto result = connectorService.createConnector(dto);

            assertNotNull(result);
            verify(vaultService).storePassword(anyString(), eq("new-password"));
            verify(connectorRepository).save(any(ReportConnector.class));
            
            // Verify getConnection was called with the password from DTO
            driverManagerMock.verify(() -> DriverManager.getConnection(anyString(), anyString(), eq("new-password")));
        }
    }

    @Test
    void deleteConnector_WithQueries_ShouldThrowException() {
        ReportConnector entity = new ReportConnector();
        entity.setId("c1");
        when(connectorRepository.findById("c1")).thenReturn(Optional.of(entity));
        when(queryRepository.findByConnectorId("c1")).thenReturn(List.of(new com.report.backend.entity.ReportQuery()));

        assertThrows(RuntimeException.class, () -> connectorService.deleteConnector("c1"));
        verify(connectorRepository, never()).deleteById(anyString());
    }

    @Test
    void deleteConnector_NoQueries_ShouldSuccess() {
        ReportConnector entity = new ReportConnector();
        entity.setId("c1");
        entity.setName("Conn1");
        when(connectorRepository.findById("c1")).thenReturn(Optional.of(entity));
        when(queryRepository.findByConnectorId("c1")).thenReturn(Collections.emptyList());

        connectorService.deleteConnector("c1");

        verify(connectorRepository).deleteById("c1");
        verify(vaultService).deletePassword("Conn1");
    }

    @Test
    void testConnection_ShouldFetchFromVaultIfPasswordBlank() throws SQLException {
        ReportConnectorDto dto = TestDataFactory.createConnectorDto();
        dto.setId("c1");
        dto.setPassword(null); // Force vault fetch
        
        ReportConnector entity = TestDataFactory.createConnectorEntity();
        entity.setName("Conn1");
        
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection mockConn = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConn);
            
            when(connectorRepository.findById("c1")).thenReturn(Optional.of(entity));
            when(vaultService.getPassword("Conn1")).thenReturn("vault-pass");

            connectorService.testConnection(dto);

            // Verify getConnection was called with the password from vault
            driverManagerMock.verify(() -> DriverManager.getConnection(anyString(), anyString(), eq("vault-pass")));
        }
    }
}
