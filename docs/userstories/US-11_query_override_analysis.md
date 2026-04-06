# User Story: Query Override and Impact Analysis during Import

## Status: Draft
**ID:** US-11

## Description
As a Report Administrator, when importing a template package (JSON), I want to be alerted if any queries in the package already exist in my environment (matched by name). I need to see the differences between the imported version and the existing version, as well as which templates are currently using the existing query, so I can safely decide whether to override the existing query or create a new one.

## Goals
- Prevent accidental duplication of queries.
- Enable global updates of shared queries across multiple templates.
- Provide visibility into the impact of overriding a query.
- Ensure mapping integrity regardless of ID differences between environments.

## Identifier Information
> [!IMPORTANT]
> Although the database uses internal IDs for runtime execution, the **exported JSON package uses Query Names and Connector Names** for all mappings. This makes the **Name** the effective identifier for matching and resolving dependencies during the import process.

## Acceptance Criteria

### 1. Conflict Detection (Analysis Phase)
- The `/api/migration/analyze` endpoint must identify query name conflicts within the target connector.
- The analysis response must include:
    - `exists`: Boolean indicating a name match.
    - `currentQueryText`: The SQL text of the existing query (for UI diffing).
    - `queryImpactMap`: A list of template names and version numbers currently mapped to that existing query.

### 2. UI Override/Create New Options
- If a conflict is detected, the UI must present two options:
    - **Override**: Use the existing query record and update its SQL text.
    - **Create New**: Create a completely new query record with a unique name.
- If no conflict is detected, only the "Create New" (or "Auto-Create") option should be used.

### 3. Impact Visibility
- Before proceeding with an import, the user must be able to see:
    - A side-by-side comparison of the imported SQL vs. the existing SQL.
    - A list of all templates hosted on the instance that will be affected by an **Override**.

### 4. Mapping Integrity (Import Phase)
- **If Override is selected**:
    - The existing `ReportQuery` record must be updated with the imported SQL text.
    - All version mappings for the imported template must be resolved by **Name** and linked to the **existing Query ID** found in the target environment.
    - All existing templates previously mapped to this Query ID must successfully generate reports using the updated SQL.
- **If Create New is selected**:
    - A new `ReportQuery` record must be created with the user's provided target name.
    - The imported template version mappings must be linked to the **newly generated Query ID**.
    - Existing templates must remain unchanged and mapped to their original records.

## Technical Notes
- The `MigrationDto` already contains fields for `currentQueryText` and `queryImpactMap`.
- The `ReportTemplateService.analyzeImport` method is already capable of populating these impact lists by searching the `TemplateQueryMappingRepository`.
- The `importTemplate` logic must be careful to resolve the correct `queryId` based on the user's selected strategy for each individual query in the package.
