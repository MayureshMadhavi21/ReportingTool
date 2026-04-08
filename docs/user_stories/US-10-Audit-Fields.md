# User Story: Standardize Audit Fields Across All Entities

**User Story ID**: US-10
**Title**: Implement Standardized Audit and Soft-Delete Fields

## Description
As a system administrator and developer, I want all database entities to have a consistent set of audit fields (who created it, when, and who last modified it) and a soft-delete flag. This will enable better tracking of data changes and safer data removal across the entire platform.

## Acceptance Criteria
1.  **Uniform Fields**: All entities in `report-service` and `connector-query-service` must include:
    *   `created_by` (String)
    *   `modified_by` (String)
    *   `created_date` (LocalDateTime)
    *   `modified_date` (LocalDateTime)
    *   `is_deleted` (Integer/Boolean, default 0/false)
2.  **Field Removal**: Existing `created_at` and `updated_at` fields must be removed.
3.  **Automatic Population**: The `created_date` and `modified_date` must be automatically populated on creation and update using JPA lifecycle hooks (`@PrePersist`, `@PreUpdate`).
4.  **Cross-DB Compatibility**: Datatypes must be compatible with H2, SQL Server, Oracle, PostgreSQL, and MySQL.
5.  **Schema Alignment**: The `schema.sql` must be updated to reflect these changes with the correct T-SQL (SQL Server) syntax.
6.  **Code Consistency**: All DTOs, Services, and Repositories referencing the old fields must be updated.

## Technical Details
- **Created Date**: Mapping to `DATETIME2` in SQL Server and `TIMESTAMP` in others.
- **Is Deleted**: Mapping to `INT` (0/1) for maximum portability across Oracle and SQL Server.
- **Audit logic**: Implement inside the entities or via a base class.
