# Database Table Structure & Explanation

This document provides a detailed explanation of the SQL Server database schema used in the Report Tool project.

## 1. System Architecture
The database is designed to support a decoupled microservices architecture where the **Connector-Query Service** handles data sourcing and the **Report Service** handles presentation logic (templates).

---

## 2. Table Definitions

### A. Data Connectivity (Connector-Query Service)

#### `rp_report_connector`
Stores metadata for external database connections.
- **Purpose**: Centralized storage of JDBC credentials and connection strings.
- **Key Columns**:
    - `db_type`: Supports SQL_SERVER, MYSQL, POSTGRESQL, and ORACLE.
    - `password_encrypted`: Stores credentials in a non-plain-text format.

#### `rp_report_query`
Stores the SQL queries that will be executed against the connectors.
- **Purpose**: Allows users to manage complex SQL logic without touching the code.
- **Key Columns**:
    - `query_text`: Uses `NVARCHAR(MAX)` to support extremely long or complex SQL scripts.

#### `rp_report_query_placeholder_metadata`
A detail table for queries.
- **Purpose**: Defines what dynamic parameters (like `:startDate` or `:region`) a query requires so the UI can prompt the user.

### B. Template Management (Report Service)

#### `rp_report_template`
The high-level container for a report project.
- **Purpose**: Organizing versions and mappings under a single name (e.g., "Monthly Financial Report").

#### `rp_report_template_version`
Enables version control for uploaded files (Word/Excel).
- **Purpose**: Allows rolling back to previous versions of a template or testing new versions in a "dev" state (`is_active = 0`).

#### `rp_template_query_mapping`
The bridge between templates and queries.
- **Purpose**: Defines how query results are mapped to JSON nodes (e.g., `json_node_name = "sales"`) for the report engine to consume.

---

## 3. Use of Indexes
Indexes are critical for maintaining performance as the database grows.

- **Primary Key Indexes**: Automatically created on `id` (VARCHAR(36)) to ensure lookups by UUID are near-instant.
- **Foreign Key Indexes (`IDX_Query_Conn`, `IDX_Map_Ver`, etc.)**: These speed up JOIN operations between tables (e.g., fetching all queries for a specific connector).
- **Performance Filtering (`IDX_Ver_Temp`)**: Speeds up filtering versions belonging to a specific template.
- **Unique Constraint Index (`UQ_Version_Node`)**: Prevents data corruption by ensuring a single JSON node name isn't used twice in the same template version.

---

## 4. Design Decisions
- **UUDs (VARCHAR(36))**: Used instead of auto-incrementing integers to ensure global uniqueness across distributed services and to facilitate easier data synchronization.
- **DATETIME2**: Used for modern precision and compatibility with Java `LocalDateTime`.

---

## 5. Entity vs. Table Discrepancy
You may notice there are **5 `@Entity` classes** in the source code but **6 tables** in the schema.

### The "@ElementCollection" Pattern
The "missing" entity is for the table **`rp_report_query_placeholder_metadata`**.

In JPA, not every table requires a dedicated Entity class. In the `ReportQuery` entity:
1.  **`ReportQuery`** is the parent Entity.
2.  It uses the **`@ElementCollection`** annotation to manage its placeholder metadata.
3.  **`PlaceholderMetadata`** is marked as **`@Embeddable`** (not `@Entity`) because it only exists as a part of a query and does not have its own independent lifecycle.
4.  JPA automatically creates the 6th table (`rp_report_query_placeholder_metadata`) to store these details and links them back to the query via `query_id`.
