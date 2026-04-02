# User Story: Version-Specific Report Generation API

## Context
As a Developer integrating the Reporting Module into other systems, I want the ability to generate reports by specifying a specific version number. This allows my module to reproduce historical reports exactly as they looked in the past, even if the "Active" production template has since changed.

## Acceptance Criteria

### 1. Versioned API Input
- The report generation endpoint (`POST /api/generate`) must accept an optional `versionNumber` parameter in the request body.
- If a `versionNumber` is provided, the system must use the `.docx` or `.xlsx` template file and the query mappings associated with that specific version.

### 2. Intelligient Defaulting
- If the `versionNumber` is **not provided**:
    - The system must prioritize the version marked as `isActive = 1`.
    - If no version is active, the system must fall back to the highest available version number.

### 3. Error Handling
- If a requested `versionNumber` does not exist for the given `templateId`, the API should return a descriptive `404 Not Found` or `400 Bad Request` error.

### 4. Format Flexibility
- The version-specific generation must still support all output formats (DOCX, PDF, XLSX) regardless of whether the source template is older.

## Technical Tasks
- [x] Verify `ReportGenerationRequestDto` includes `versionNumber`.
- [x] Verify `ReportGenerationService` logic for version selection.
- [ ] Implement "Test Generation" UI in `TemplateDetails.tsx` to allow manual verification of this API.
