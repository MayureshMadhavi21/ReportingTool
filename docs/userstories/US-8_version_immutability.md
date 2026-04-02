# User Story: Enforcing Version Immutability

## Context
As an Administrator, I want to ensure that historical template versions remain immutable (read-only) so that I have a reliable audit trail of past report configurations. Only the currently active version should be editable.

## Acceptance Criteria

### 1. Conditional Action Visibility
- In the **Version History** table, the **Edit Version (Pencil)** icon must only be visible/active for the version where `isActive === 1`.
- For all archived versions (`isActive === 0`), the edit option must be removed or hidden.

### 2. Navigation Restrictions
- If a user manually attempts to navigate to `/templates/:id/versions/:archivedVersionId/edit`, they should be redirected back to the view mode or shown an error message (Optional, UI primary focus).

### 3. Snapshot Integrity
- The **View Version (Eye)** and **Download Snapshot** icons should remain available for all versions (active and archived) to allow inspection of historical data mappings.

## Technical Tasks
- [ ] Update `TemplateDetails.tsx` version table to conditionally render the "Edit" ActionIcon.
- [ ] Ensure the "Delete" button logic is preserved (only disabled for the active version).
