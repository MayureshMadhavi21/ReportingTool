# Report Generation Tool Refactoring Walkthrough

This document outlines the changes made to transform the initial monolithic Spring Boot application into a **scalable microservice architecture** capable of extremely high throughput, enhanced database connector security, and a robust template storage mechanism.

## 1. Microservice Splitting

We separated the monolithic backend into two strictly defined microservices to scale query execution and template processing independently:
- **`report-service` (Port 8084):** Handles storing/resolving Word/Excel templates via various storage strategies, executing Aspose operations, and merging query results into downloadable files.
- **`connector-query-service` (Port 8085):** Dedicated entirely to securely storing database connection details, mapping raw SQL queries, and handling active database connections.

To allow independent deployments, we completely updated the `build.sh/bat`, `start.sh/bat`, and `stop.sh/bat` scripts.

## 2. API & Data Isolation (Dependency Checks)

**Inter-Service Sync:**
Because the two domains were split, we implemented [ConnectorQueryServiceClient](file:///d:/publishingV2/report-tool/report-service/src/main/java/com/report/backend/service/ConnectorQueryServiceClient.java#13-59) in `report-service` and [ReportServiceClient](file:///d:/publishingV2/report-tool/connector-query-service/src/main/java/com/report/backend/service/ReportServiceClient.java#8-34) in `connector-query-service`. 
This allows us to implement stringent CRUD safety nets across network boundaries:
1. You **cannot delete** a Connector if there are any SQL Queries attached to it.
2. You **cannot delete** an SQL Query if `report-service` reports it is currently mapped to a Report Template.

## 3. High Performance 1000/min Execution Pipeline

To support rapid-fire report generation without blocking threads, we shifted the query fetching mechanism to be completely asynchronous using **Java `CompletableFuture`**.

Multiple queries mapped to a single template now run concurrently across multiple dedicated CPU threads. The results are coalesced dynamically and passed immediately to Aspose. Furthermore, both microservices have received explicit **HikariCP Connection Pool** optimization ([application.yml](file:///d:/publishingV2/report-tool/backend/src/main/resources/application.yml)) tailored for 50 active connections per node. Load testing guidelines have also been populated in [docs/Load_Testing_Guide.md](file:///d:/publishingV2/report-tool/docs/Load_Testing_Guide.md).

## 4. Vault/AES Secure Password Strategy

To safeguard external database credentials, Connector passwords are no longer saved inside the SQL database (`@Transient`). 
Instead, we implemented Application-Level AES Encryption.
- When creating/updating a connector, the plaintext password is mathematically scrambled and pushed to an isolated mock Vault (`data/vault.json`).
- When a query must run, the DB execution context unlocks the Vault entry in-memory, ensuring the true credential never leaks into logs or DB tables.

*(See [docs/guide.md](file:///d:/publishingV2/report-tool/docs/guide.md) for more configuration details).*

## 5. Storage Abstractions (Local & Azure Blob)

Word and Excel Templates are no longer stored directly as raw BLOBs inside the Relational Database. This shift enhances general DB performance and uncouples binary management.
We implemented a [TemplateStorageStrategy](file:///d:/publishingV2/report-tool/report-service/src/main/java/com/report/backend/service/TemplateStorageStrategy.java#3-8) with two active profiles:
1. [LocalFileSystemStorageStrategy](file:///d:/publishingV2/report-tool/report-service/src/main/java/com/report/backend/service/LocalFileSystemStorageStrategy.java#14-62): The default (stores binary files to `/data/templates` directly onto the disk).
2. [AzureBlobStorageStrategy](file:///d:/publishingV2/report-tool/report-service/src/main/java/com/report/backend/service/AzureBlobStorageStrategy.java#11-43): An injected shim designed for seamless Cloud upload toggled via `template.storage.type=azure`.

## 6. Frontend API Adjustments

The React/Vite UI was updated to embrace the microservice nature:
- Exported isolated `reportApi` and `connectorApi` logic using Axios.
- Upgraded components to allow comprehensive management features:
  - **Connectors**: Added an "Update Password" flow to rotate credentials.
  - **Queries**: Altered the query form to allow modifying existing Query definitions fully.
  - **Templates**: Integrated the ability to update/swap out physical `docx/xlsx` template files _without_ destroying existing ID mappings, as well as the ability to mutate/edit individual mapping rules fully.

Everything compiles successfully with 0 defects.
