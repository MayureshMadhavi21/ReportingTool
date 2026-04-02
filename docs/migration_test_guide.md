# Template Migration Test Guide

This guide outlines the manual test cases for the Template Migration functionality (Export & Import) between environments.

## Overview
The migration tool allows exporting a template, its versions, and its dependencies (Connectors and Queries) into a single JSON file, which can then be imported into another environment with granular conflict resolution.

---

## 1. Export Functionality
**Goal:** Verify that a template and its dependencies are correctly packaged into a JSON file.

| Test Case ID | Step | Expected Result |
| :--- | :--- | :--- |
| **EXPORT-01** | Open Template Details page and click **Export Template**. | A JSON file is downloaded immediately. |
| **EXPORT-02** | Open the exported JSON file. | Verify it contains `templateName`, `description`, `versions`, `connectors`, and `queries`. |
| **EXPORT-03** | Verify `fileContentBase64` in the JSON. | Ensure each version has a non-empty Base64 string representing the file. |
| **EXPORT-04** | Verify `fileName` in the JSON. | Ensure the original filename (e.g., `Template.docx`) is preserved in the metadata for each version. |

---

## 2. Import Functionality - Conflict Resolution
**Goal:** Test the three strategies: `SKIP`, `OVERRIDE`, and `CREATE_NEW`.

### A. Core Template Resolution
| Test Case ID | Scenario | Strategy | Expected Result |
| :--- | :--- | :--- | :--- |
| **IMPORT-T1** | Template doesn't exist. | `CREATE_NEW` | A new template is created with the target name. |
| **IMPORT-T2** | Template exists. | `SKIP` | Uses the existing template ID. No metadata (name/description) is changed. |
| **IMPORT-T3** | Template exists. | `OVERRIDE` | **CAUTION:** Existing versions of the template are DELETED (DB + FS). New versions from the JSON are imported. |

### B. Connector Resolution
| Test Case ID | Scenario | Strategy | Expected Result |
| :--- | :--- | :--- | :--- |
| **IMPORT-C1** | Connector exists. | `SKIP` | Uses the existing connector. URL/Username remain unchanged. |
| **IMPORT-C2** | Connector exists. | `OVERRIDE` | Existing connector is updated with the URL and Username from the JSON. **Password must be provided.** |
| **IMPORT-C3** | New name given. | `CREATE_NEW` | A completely new connector is created. **Password must be provided.** |

### C. Query Resolution
| Test Case ID | Scenario | Strategy | Expected Result |
| :--- | :--- | :--- | :--- |
| **IMPORT-Q1** | Query exists. | `SKIP` | Uses the existing query. SQL/Query text remains unchanged. |
| **IMPORT-Q2** | Query exists. | `OVERRIDE` | Existing query SQL/Text is updated to match the exported JSON content. |
| **IMPORT-Q3** | New name given. | `CREATE_NEW` | A new query is created under the (possibly new) connector. |

---

## 3. End-to-End Migration Flow (Manual Steps)

### Step 1: Pre-Import Validation
1.  Navigate to **Migration > Import**.
2.  Upload the exported JSON file.
3.  **UI Verification:** Ensure the UI correctly lists all found Connectors, Queries, and the Template from the file.
4.  **Action:** Select strategies for each item.

### Step 2: Execution & Passwords
1.  If selecting `OVERRIDE` or `CREATE_NEW` for a connector, ensure the **Password** field becomes mandatory/visible.
2.  Enter valid credentials for the target environment databases.
3.  Click **Confirm Import**.

### Step 3: Post-Import Verification
1.  **Template List:** Search for the imported template. Verify it appears.
2.  **Version History:** Open Template Details. Verify all versions from the source are present.
3.  **Mappings:** Check individual versions. Ensure Queries and Connectors are correctly mapped (even if names were changed during import).
4.  **File Integrity:** Download one of the imported versions. Verify it opens correctly (DOCX/XLSX) and isn't corrupted.

---

## 4. Edge Cases & Error Handling
| Test Case ID | Scenario | Expected Result |
| :--- | :--- | :--- |
| **ERR-01** | Upload invalid/corrupted JSON. | UI should show an "Invalid Format" error and stop processing. |
| **ERR-02** | `OVERRIDE` connector without password. | Backend should return a validation error (or UI should block submission). |
| **ERR-03** | `SKIP` a connector that doesn't actually exist in the target. | System should show an error: "Target Connector [Name] not found for Skip strategy". |
| **ERR-04** | Import template with conflicting version numbers. | System should handle versioning gracefully (the current logic Re-creates versions sequentially based on the source). |

---

> [!IMPORTANT]
> **OVERRIDE Caution:** When overriding an existing template, all current versions in the target system for that specific template name will be wiped to ensure a clean sync with the source system. Always back up the target if unsure.

> [!TIP]
> Use different target names (e.g., `MyTemplate_V2`) during `CREATE_NEW` to test side-by-side versions of the same template logic.
