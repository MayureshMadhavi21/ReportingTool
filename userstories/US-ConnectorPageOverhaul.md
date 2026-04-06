# User Story: Dedicated Connector Management Pages

## Overview
As an administrator managing multiple database connections, I want to have dedicated, full-size pages for creating, editing, and viewing database connectors. This will allow for better organization of complex details like JDBC builders and secure authentication configurations without the cramped feel of a modal popup.

## Acceptance Criteria
- [ ] **Dedicated Routes**:
    - [ ] `/connectors/add` (New Connector)
    - [ ] `/connectors/:id/edit` (Edit Connector)
    - [ ] `/connectors/:id/view` (View Connector Details)
- [ ] **Connector Editor Component**:
    - [ ] Create a comprehensive `ConnectorEditor.tsx` page to handle these workflows.
    - [ ] Move the **JDBC Connection Builder** logic from `Connectors.tsx` to the new component.
    - [ ] Handle **Read-only Mode** for the view route.
- [ ] **Table Actions Update**:
    - [ ] "Add Connector" button in `Connectors.tsx` navigates to `/connectors/add`.
    - [ ] Table action icons (View/Edit/Delete) navigate to the appropriate dedicated pages.
- [ ] **UX Consistency**:
    - [ ] Each page should have breadcrumbs and a clear title.
    - [ ] Use a consistent layout for all Connector pages.

## Technical Tasks
- [ ] Add routes to `App.tsx`: `/connectors/add`, `/connectors/:id/edit`, `/connectors/:id/view`.
- [ ] Create `ConnectorEditor.tsx` with all the current connector logic.
- [ ] Refactor `Connectors.tsx` to remove the Modal and link to the new routes.
- [ ] Ensure functional parity with existing features like **Test Connection** and **URL builders**.
