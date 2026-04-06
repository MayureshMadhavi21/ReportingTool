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
    private static final Map<String, String> DRIVER_MAP = new HashMap<>();

    static {
        DRIVER_MAP.put("SQL_SERVER", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DRIVER_MAP.put("MYSQL", "com.mysql.cj.jdbc.Driver");
        DRIVER_MAP.put("POSTGRESQL", "org.postgresql.Driver");
        DRIVER_MAP.put("ORACLE", "oracle.jdbc.OracleDriver");
        DRIVER_MAP.put("H2", "org.h2.Driver");
    }

    private void loadDriver(String dbType) {
        if (dbType == null)
            return;
        String driverClass = DRIVER_MAP.get(dbType.toUpperCase());
        if (driverClass != null) {
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                // For H2 and MSSQL, they are usually in classpath so we proceed if
                // Class.forName fails
                // but for Oracle/MySQL specifically, we want a clear error if driver is missing
                if ("ORACLE".equals(dbType.toUpperCase()) || "MYSQL".equals(dbType.toUpperCase())
                        || "POSTGRESQL".equals(dbType.toUpperCase())) {
                    throw new RuntimeException("JDBC Driver for " + dbType + " not found: " + driverClass
                            + ". Please ensure the dependency is in pom.xml and re-build.");
                }
            }
        }
    }

    public List<Map<String, Object>> executeQuery(String queryName, String dbType, String jdbcUrl, String username,
            String password, String queryText, Map<String, Object> params,
            Map<String, com.report.backend.entity.PlaceholderMetadata> placeholderMetadata) {
        loadDriver(dbType);

        // US-6: SQL Injection & Security Check
        // PreparedStatement inherently protects against SQL injection. As
        // defense-in-depth, we also block destructive operations.
        String upperQuery = queryText.toUpperCase();
        if (upperQuery.matches(".*\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|GRANT|REVOKE|EXEC|EXECUTE)\\b.*")) {
            throw new RuntimeException("Security Violation: Only read statements (SELECT) are permitted.");
        }

        // US-9: Strict Type Validation before execution
        if (params != null && placeholderMetadata != null) {
            for (Map.Entry<String, com.report.backend.entity.PlaceholderMetadata> entry : placeholderMetadata
                    .entrySet()) {
                String paramName = entry.getKey();
                String expectedType = entry.getValue().getType();
                Object value = params.get(paramName);

                if (value != null) {
                    validateType(paramName, value, expectedType);
                }
            }
        }

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

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            try {
                conn.setReadOnly(true); // US-6: Enforce read-only connection
            } catch (Exception ignored) {
                // Ignore if the driver does not support read-only mode
            }
            try (PreparedStatement stmt = conn.prepareStatement(parsedQuery.toString())) {
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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + queryName + ". Error: " + e.getMessage(), e);
        }

        return results;
    }

    public void validateQuery(String dbType, String jdbcUrl, String username, String password, String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            throw new RuntimeException("SQL Syntax Validation Failed: Query text cannot be empty.");
        }

        loadDriver(dbType);

        // US-6: SQL Injection & Security Check
        String upperQuery = queryText.toUpperCase().trim();
        if (upperQuery.matches(".*\\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|GRANT|REVOKE|EXEC|EXECUTE)\\b.*")) {
            throw new RuntimeException(
                    "Security Violation: Only read statements (SELECT) are permitted. Destructive keywords found.");
        }

        // Let the actual database driver validate the query structure in the next step.

        Matcher matcher = PARAM_PATTERN.matcher(queryText);
        StringBuilder parsedQuery = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            parsedQuery.append(queryText, lastEnd, matcher.start());
            parsedQuery.append("?");
            lastEnd = matcher.end();
        }
        parsedQuery.append(queryText.substring(lastEnd));

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            // Preparing the statement asks the database engine to parse and compile the
            // SQL.
            // Some drivers (like H2, SQL Server, Postgres) are "lazy" and don't fully
            // validate until execution.
            // Calling getMetaData() forces a deeper dry-run check without actually running
            // the query.
            try (PreparedStatement stmt = conn.prepareStatement(parsedQuery.toString())) {
                stmt.getMetaData();
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQL Syntax Validation Failed: " + e.getMessage(), e);
        }
    }

    private void validateType(String name, Object value, String type) {
        if (type == null || "STRING".equalsIgnoreCase(type))
            return;

        String valStr = value.toString();
        try {
            switch (type.toUpperCase()) {
                case "NUMBER":
                    Double.parseDouble(valStr);
                    break;
                case "DATE":
                    // Simple check for YYYY-MM-DD or similar formats if it's a string
                    if (value instanceof String) {
                        if (!valStr.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                            throw new RuntimeException("Format mismatch");
                        }
                    }
                    break;
                case "BOOLEAN":
                    if (!valStr.equalsIgnoreCase("true") && !valStr.equalsIgnoreCase("false")) {
                        throw new RuntimeException("Invalid boolean");
                    }
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid value for placeholder '" + name + "': Expected " + type
                    + ", but received '" + valStr + "'.");
        }
    }
}
