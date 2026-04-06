# User Story: SQL Query Syntax Validation & UX Enhancement

## Overview
As a developer using the Report Tool, I want my SQL queries to be validated against the selected connector type during creation/editing, and I want a premium code-editing experience so that I can write correct queries efficiently without errors or confusion.

## Acceptance Criteria
- [ ] **SQL Syntax Validation**:
    - [ ] Backend should catch syntax errors during validation (e.g., missing `FROM`, incomplete `WHERE` clauses, dangling `AND`).
    - [ ] Validation should be connector-aware (validated against the actual database engine).
- [ ] **Query Editor UX**:
    - [ ] "Add Query" is now a separate page instead of a popup modal.
    - [ ] The SQL Query text box is **disabled** until a connector is selected.
    - [ ] The "Save Query" button is **disabled** until:
        - Connector is selected.
        - Query Name is filled.
        - SQL Query text is provided.
        - Validation has been successfully performed on the current query.
- [ ] **Premium Code Editor**:
    - [ ] Replace the standard textarea with a rich code editor.
    - [ ] Editor should display **line numbers**.
    - [ ] SQL keywords should be syntax-highlighted (different font colors).
    - [ ] Placeholders (`:name`) should have a unique color distinct from SQL keywords.
    - [ ] Mantine-consistent styling for the editor itself.

## Technical Tasks
- [ ] **Backend**:
    - Update `DatabaseExecutionService.java` to use more robust validation logic (e.g., calling `getMetaData()` on prepared statements).
- [ ] **Frontend**:
    - Add routing for Add/Edit Query pages in `App.tsx`.
    - Create `QueryEditor` component with syntax highlighting and line numbers.
    - Implement state management for form validation status.
    - Refactor `Queries.tsx` to link to the new pages.
