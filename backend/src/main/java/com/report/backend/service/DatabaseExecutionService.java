package com.report.backend.service;

import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class DatabaseExecutionService {

    public List<Map<String, Object>> executeQuery(String queryName, String jdbcUrl, String username, String password, String queryText) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(queryText);
             ResultSet rs = stmt.executeQuery()) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + queryName + ". Error: " + e.getMessage(), e);
        }
        
        return results;
    }
}
