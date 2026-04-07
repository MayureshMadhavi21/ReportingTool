package com.report.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseExecutionServiceTest {

    @InjectMocks
    private DatabaseExecutionService executionService;

    @Test
    void executeQuery_ValidSelect_ShouldReturnResults() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection conn = mock(Connection.class);
            PreparedStatement stmt = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);

            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(stmt);
            when(stmt.executeQuery()).thenReturn(rs);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnCount()).thenReturn(1);
            when(metaData.getColumnLabel(1)).thenReturn("col1");
            when(rs.next()).thenReturn(true, false);
            when(rs.getObject(1)).thenReturn("value1");

            List<Map<String, Object>> result = executionService.executeQuery(
                    "test", "H2", "jdbc:h2:mem:test", "sa", "pass",
                    "SELECT * FROM test WHERE id = :id",
                    Map.of("id", 1),
                    new HashMap<>()
            );

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("value1", result.get(0).get("col1"));
            verify(stmt).setObject(eq(1), eq(1));
        }
    }

    @Test
    void executeQuery_DbError_ShouldThrowException() throws SQLException {
        SQLException sqlException = new SQLException("Conn Failed");
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenThrow(sqlException);

            assertThrows(RuntimeException.class, () -> 
                executionService.executeQuery("test", "H2", "url", "user", "pass", "SELECT 1", null, null)
            );
        }
    }

    @Test
    void executeQuery_SecurityViolation_ThrowsException() {
        assertThrows(RuntimeException.class, () -> 
            executionService.executeQuery("test", "H2", "url", "user", "pass", 
                    "DELETE FROM users", null, null)
        );
    }

    @Test
    void validateQuery_ValidQuery_ShouldComplete() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection conn = mock(Connection.class);
            PreparedStatement stmt = mock(PreparedStatement.class);

            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(stmt);

            assertDoesNotThrow(() -> 
                executionService.validateQuery("H2", "url", "user", "pass", "SELECT 1")
            );
        }
    }

    @Test
    void validateQuery_InvalidQueryText_ThrowsException() {
        assertThrows(RuntimeException.class, () -> 
            executionService.validateQuery("H2", "url", "user", "pass", "")
        );
    }

    @Test
    void validateQuery_DestructiveSql_ThrowsException() {
        assertThrows(RuntimeException.class, () -> 
            executionService.validateQuery("H2", "url", "user", "pass", "DROP TABLE users")
        );
    }
}
