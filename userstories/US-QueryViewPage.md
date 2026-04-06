# User Story: Dedicated View Page for Queries

## Overview
As a developer using the Report Tool, I want to be able to view the full details of a registered SQL query on a dedicated page. This will allow me to review the query syntax, placeholders, and connector mapping in a focused view without the risk of accidental edits.

## Acceptance Criteria
- [ ] **Restored View Action**:
    - [ ] The "View" (Eye icon) action is restored in the Query list table in `Queries.tsx`.
- [ ] **Dedicated View Page**:
    - [ ] Clicking the "View" icon navigates to `/queries/:id/view`.
    - [ ] The page uses the same layout as the Add/Edit page but in **read-only mode**.
    - [ ] All input fields (Connector, Name, SQL, Description, Placeholders) are disabled.
    - [ ] The "Save" and "Validate" buttons are hidden; only a "Close" or "Back" button is visible.
- [ ] **URL Consistency**:
    - [ ] Routing is updated in `App.tsx` to support the view path.

## Technical Tasks
- [ ] Update `App.tsx` with `/queries/:id/view` route.
- [ ] Enhace `QueryEditor.tsx` to accept a `viewOnly` prop and handle read-only state.
- [ ] Update `Queries.tsx` to include the View action link.
