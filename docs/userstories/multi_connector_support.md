# User Stories: Multi-Database Connector Support

## Current State (Context)
The system already has a `dbType` field (`SQL_SERVER`, `MYSQL`, `POSTGRESQL`, `ORACLE`, `H2`) in the entity
and a raw JDBC URL text box in the UI. However:
- Only **MS SQL** JDBC driver is bundled (`mssql-jdbc`) — Oracle/MySQL have no driver dependency
- The UI shows a raw JDBC URL text box with no guidance — easy to make mistakes
- There is no URL builder, no type-specific port defaults, no driver class validation
- The export/import migration carries `dbType` but there's no enforcement or documentation

---

## US-1: Type-Aware JDBC URL Builder in the Connector Form
**As a** user creating a connector,
**I want** the system to show a guided form with host, port, database name, and schema fields when I select a database type,
**So that** I never have to manually construct error-prone JDBC URLs.

### Acceptance Criteria
- [ ] When user selects a DB Type, the form switches to **structured fields**: Host, Port, Database Name, (optional) Schema/SID/Service Name
- [ ] Default port is pre-filled based on type: MS SQL → `1433`, Oracle → `1521`, MySQL → `3306`, PostgreSQL → `5432`
- [ ] The system **auto-generates the JDBC URL** in a read-only preview:
  - MS SQL: `jdbc:sqlserver://host:port;databaseName=db;encrypt=true;trustServerCertificate=true`
  - Oracle: `jdbc:oracle:thin:@host:port:sid` or `jdbc:oracle:thin:@//host:port/service`
  - MySQL: `jdbc:mysql://host:port/database?useSSL=false&serverTimezone=UTC`
  - PostgreSQL: `jdbc:postgresql://host:port/database`
- [ ] Advanced users can toggle a **"Use Custom JDBC URL"** switch to directly enter a raw URL
- [ ] The generated URL is what gets saved to the database

---

## US-2: Bundle Required JDBC Drivers for All Supported Databases
**As a** system administrator,
**I want** the application to include JDBC driver dependencies for MS SQL, Oracle, MySQL, and PostgreSQL,
**So that** connection testing and query execution work without any manual driver installation.

### Acceptance Criteria
- [ ] `pom.xml` in `connector-query-service` includes driver dependencies:
  - MS SQL: `com.microsoft.sqlserver:mssql-jdbc` ✅ (already present)
  - MySQL: `com.mysql:mysql-connector-j`
  - PostgreSQL: `org.postgresql:postgresql`
  - Oracle: `com.oracle.database.jdbc:ojdbc11` (requires Oracle Maven repo / manual install)
- [ ] `DatabaseExecutionService` loads the correct driver class explicitly via `Class.forName()` before connection
- [ ] If driver is missing at runtime, error is: `"Driver for Oracle is not available. Contact your administrator."`

---

## US-3: Type-Specific Connection Test Validation
**As a** user creating or editing a connector,
**I want** the "Test Connection" button to validate not just connectivity but also type-specific constraints,
**So that** I can be confident the connector will work for query execution.

### Acceptance Criteria
- [ ] `ReportConnectorService.testConnection()` pre-validates the JDBC URL format against the declared `dbType`
- [ ] On success, the test response includes DB metadata: server version, connected database name
- [ ] On failure, the error message includes the DB type and the specific JDBC exception cause
- [ ] The UI shows a success card: `Connected ✅ — Oracle 19c | Database: REPORTING_DB | User: ADMIN`

---

## US-4: DB Type Badge in the Connector List & Template Details
**As a** user reviewing configured connectors,
**I want** the connector type displayed as a colour-coded badge in all connector lists,
**So that** I can quickly identify which technology each connector uses at a glance.

### Acceptance Criteria
- [ ] The `Connectors` list page shows a coloured badge per type:
  - MS SQL → 🔵 Blue — "MS SQL"
  - Oracle → 🔴 Red — "Oracle"
  - MySQL → 🟠 Orange — "MySQL"
  - PostgreSQL → 🟣 Purple — "PostgreSQL"
- [ ] Same badges appear in `TemplateDetails` mapping section and in the Migration Wizard
- [ ] The raw `dbType` string (`SQL_SERVER`) is never shown to users — always mapped to human-readable label

---

## US-5: Oracle-Specific Authentication Modes
**As a** DBA configuring an Oracle connector,
**I want** to choose between SID and Service Name connection modes,
**So that** I can connect to modern Oracle RAC/PDB databases correctly.

### Acceptance Criteria
- [ ] When DB Type = Oracle, the form shows a radio toggle: "Connect via SID" / "Connect via Service Name"
- [ ] SID mode generates: `jdbc:oracle:thin:@host:port:sid`
- [ ] Service Name mode generates: `jdbc:oracle:thin:@//host:port/service_name`
- [ ] The selected mode is preserved in the saved JDBC URL

---

## US-6: Windows Authentication for MS SQL
**As a** user connecting to a corporate MS SQL Server using Windows Authentication,
**I want** to mark a connector as using integrated/Windows security instead of SQL login,
**So that** I do not need to store a password in the vault.

### Acceptance Criteria
- [ ] When DB Type = MS SQL, an optional checkbox: **"Use Windows Authentication (Integrated Security)"**
- [ ] When checked, the JDBC URL includes `integratedSecurity=true` and username/password fields are hidden
- [ ] The backend skips vault password lookup and passes the URL directly to `DriverManager`

---

## US-7: Migration Export/Import Carries DB Type Correctly
**As a** DevOps engineer migrating a template package between environments,
**I want** the exported JSON to include full DB type information for each connector,
**So that** the target environment can present the correct connector form when importing.

### Acceptance Criteria
- [ ] The `ExportedConnectorDto` includes `dbType`, `host`, `port`, `databaseName` (parsed from JDBC URL)
- [ ] During import, the connector form pre-fills the structured fields from the export data
- [ ] User only needs to enter the password; all other fields are pre-populated

---

## Implementation Priority

| Story | Priority | Status |
|---|---|---|
| US-1: JDBC URL Builder | 🔴 High | Pending |
| US-2: Driver Dependencies | 🔴 High | Pending |
| US-3: Type-Specific Test | 🔴 High | Pending |
| US-4: DB Type Badges | 🟡 Medium | Pending |
| US-5: Oracle SID/Service | 🟡 Medium | Pending |
| US-6: Windows Auth (MS SQL) | 🟢 Low | Pending |
| US-7: Migration Type Support | 🟡 Medium | Pending |
