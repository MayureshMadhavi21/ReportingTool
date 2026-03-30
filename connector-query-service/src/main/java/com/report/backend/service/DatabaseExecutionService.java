package com.report.backend.service;

import com.report.backend.entity.ReportConnector;
import com.report.backend.entity.ReportQuery;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatabaseExecutionService {

    private static final Pattern PARAM_PATTERN = Pattern.compile(":(\\w+)");

    public List<Map<String, Object>> executeQuery(String queryName, String jdbcUrl, String username, String password, String queryText, Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        List<String> paramOrder = new ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(queryText);
        StringBuilder parsedQuery = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            parsedQuery.append(queryText, lastEnd, matcher.start());
            parsedQuery.append("?");
            paramOrder.add(matcher.group(1));
            lastEnd = matcher.end();
        }
        parsedQuery.append(queryText.substring(lastEnd));

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement(parsedQuery.toString())) {
            
            for (int i = 0; i < paramOrder.size(); i++) {
                String paramName = paramOrder.get(i);
                Object value = params != null ? params.get(paramName) : null;
                stmt.setObject(i + 1, value);
            }

            try (ResultSet rs = stmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + queryName + ". Error: " + e.getMessage(), e);
        }
        
        return results;
    }
}
