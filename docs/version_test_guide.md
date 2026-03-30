# Enterprise Template Versioning: Technical & Stability Guide

This guide documents the **Database-Independent Template Versioning Engine** and provides a step-by-step procedure to validate its stability and cross-database compatibility (SQL Server, MySQL, Oracle, H2).

---

## 🏗️ Core Architecture Overview

The system transitions from "one-off uploads" to an **Immutable Snapshot Model**. Every template change is tracked through numerical versioning with strict state management.

### Key Logic & Rules:
1.  **Snapshot Activation**: Uses numeric flags (`0` for Inactive, `1` for Active/Production) instead of booleans to ensure portability across various SQL drivers.
2.  **File-Driven Versions**: A new version (`vN + 1`) is created **only** when the physical `.docx` or `.xlsx` file is uploaded. 
3.  **Mapping Persistence**: Mapping updates (adding/removing queries) update the **same** active version without triggering a version increment.
4.  **Inheritance**: When a new version is created (v2), it automatically inherits/clones all query mappings from the previous version (v1) to save configuration time.
5.  **Continuity Rule**: If the active production version is purged, the engine automatically promotes the next highest available revision to `Active` to ensure reporting systems never break.

---

## 🧪 Validation & Test Steps

### Phase 1: Initial Lifecycle (v1)
1.  **Create Template**: Go to "Report Inventory" and click `Create Template`. Upload a `.docx` file.
2.  **Verification**: 
    - [ ] Navigating to the template record shows `v1` and "Active (Prod)".
    - [ ] Database shows `is_active = 1` and `version_number = 1`.
3.  **Mapping**: Click `Edit Workshop`. Add 2-3 queries to the "Mapping Workshop" section.
4.  **Verification**: Navigate to `Generate`. Select the template. Verify all 2-3 query parameters appear.

### Phase 2: Revision Rollover (v2)
1.  **Update File**: In the `Edit Workshop` for your template, use the `Snapshot Control` card to upload a revised file.
2.  **Verification**: 
    - [ ] Version number is now `v2`.
    - [ ] Mappings from v1 have been cloned to v2 automatically.
    - [ ] Snapshot Log (History) shows v1 is now "Archived" and v2 is "Active".

### Phase 3: Rollback & Promotion
1.  **Access History**: Go to the `Snapshot Log` / `All Versions` page.
2.  **Promote v1**: Find the v1 record. In actions, select `Promote to Production`.
3.  **Verification**:
    - [ ] v1 status changes to "Active (Prod)".
    - [ ] v2 status changes to "Archived".
    - [ ] The generation page now uses the v1 file and v1 mappings.

### Phase 4: Intelligent Parameter Deduplication
1.  **Cross-Query Mapping**: In the `Edit Workshop` for a version, map TWO different queries that both use a parameter named `batch_id`.
2.  **Global Input Test**: Go to the `Report Engine` (Generate) page.
3.  **Verification**:
    - [ ] Even though 2 queries require `batch_id`, the UI shows only **ONE** input field for `Batch_id`.
    - [ ] Generating the report correctly distributes that single value to both backend queries.

### Phase 5: Resource Cleanup (Purge)
1.  **Archive Deletion**: In the `Snapshot Log`, delete a version that is "Archived".
2.  **Verification**: Record is removed and physical file is deleted from storage.
3.  **Active Guard**: Attempt to delete the version marked as "Active".
4.  **Verification**: The system must prevent this action with an error notification.

---

## 🛠️ Data Type Verification (Architectural Guardrails)
To ensure database independence, verify the following in your database inspector:
- **`ReportTemplateVersion.is_active`**: Should be `INT` or `NUMBER`, not `BIT` or `BOOLEAN`.
- **`ReportTemplateVersion.version_number`**: Should be `INT`.
- **Primary Keys**: Should be `VARCHAR(36)` or similar strings (UUIDs generated in Java).
