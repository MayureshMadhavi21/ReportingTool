# Full-Stack Report Generation Tool - Walkthrough

## Overview
I have successfully built a complete full-stack **Report Generation Tool** using Java Spring Boot (Backend) and React with Mantine UI (Frontend). The architecture follows configuration-driven design where external data sources and dynamic SQL queries can be registered via the UI and mapped directly to Aspose-compatible Word/Excel documents.

## Changes Made
1. **Database Schema Setup (SQL Server DDL)**
   - Created the core database schema in [d:\publishingV2\report-tool\docs\schema.sql](file:///d:/publishingV2/report-tool/docs/schema.sql).
   - Built a fully normalized footprint covering: `Report_Connector`, `Report_Query`, `Report_Template`, and `Template_Query_Mapping` tables.
   - The tables support cross-database dynamics while keeping credentials and data flows secure.

2. **Backend Application (Spring Boot 3.x + Java 17)**
   - Initialized a Maven project with dependencies for JPA, MSSQL, H2, and Aspose Words/Cells.
   - Built switchable profiling configurations: [application-h2.yml](file:///d:/publishingV2/report-tool/backend/src/main/resources/application-h2.yml) (for immediate local tests) and [application-sqlserver.yml](file:///d:/publishingV2/report-tool/backend/src/main/resources/application-sqlserver.yml) (for production intent).
   - Designed a robust [DatabaseExecutionService](file:///d:/publishingV2/report-tool/backend/src/main/java/com/report/backend/service/DatabaseExecutionService.java#10-42) that spins up localized, dynamic JDBC connections (bypassing the internal JPA EntityManager) to run custom user SQL queries on remote databases.
   - Built [ReportGenerationService](file:///d:/publishingV2/report-tool/backend/src/main/java/com/report/backend/service/ReportGenerationService.java#17-79) utilizing **CompletableFuture Parallelism** to run all registered queries for a template at once and aggregate the outcomes into a cohesive JSON tree structure.
   - Provided complete REST controllers with integrated OpenAPI / Swagger 3 documentation accessible at `/swagger-ui.html`.

3. **Frontend Application (React + Vite + Mantine UI)**
   - Configured a clean Vite TS stack styled exclusively in custom `#f8f9fa` backgrounds and dynamic Mantine elements.
   - [Connectors.tsx](file:///d:/publishingV2/report-tool/frontend/src/pages/Connectors.tsx): Complete CRUD for maintaining external database JDBC strings.
   - [Queries.tsx](file:///d:/publishingV2/report-tool/frontend/src/pages/Queries.tsx): Editor to formulate SQL requests against designated Connectors.
   - [Templates.tsx](file:///d:/publishingV2/report-tool/frontend/src/pages/Templates.tsx): Implemented `multipart/form-data` uploads with a specialized Modal strictly designed to let you associate multiple parameterized queries to an Aspose Template Node (`json_node_name`).
   - [Generate.tsx](file:///d:/publishingV2/report-tool/frontend/src/pages/Generate.tsx): A one-click panel triggering the comprehensive download pipeline capable of morphing the output via Aspose into DOCX, PDF, or XLSX format.

## Verification & Usage
All unit tests in the backend, specifically assessing parallel `CompletableFuture` compilation against the mockup Aspose Engine, passed smoothly via `mvn clean test`.

### How to use the solution locally:

**1. Start the Backend:**
```powershell
cd d:\publishingV2\report-tool\backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
This runs the backend purely in-memory using H2. The Swagger API is natively running on `http://localhost:8084/swagger-ui.html`.

**2. Start the Frontend:**
```powershell
cd d:\publishingV2\report-tool\frontend
npm run dev
```
Open `http://localhost:5173`. 

**3. Test Scenario:**
1. Navigate to **Connectors** on the Frontend and add an H2 Connector pointing to `jdbc:h2:mem:reportdb` (using username `sa` and leaving the password blank). (Mocking a self-connection).
2. Navigate to **Queries** and add a query on that Connector: `SELECT name, file_type FROM Report_Template`
3. Navigate to **Templates**, upload a blank mock Word `.docx` file containing the Aspose tag: `<<foreach [t in templates]>><<[t.name]>> - <<[t.file_type]>><</foreach>>`.
4. Click "+ Add Array Mapping" and map the query you devised to the `templates` node.
5. Head to **Generate Report**, select the template, and hit download! The backend will parallelize your SQL, consolidate a structured JSON mapping to `{"templates": [{"name": ..., "file_type": ...}]}`, and invoke Aspose to inject it straight into your document.
