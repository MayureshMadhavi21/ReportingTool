# Template Migration — User Stories

**Epic:** As a DevOps or Report Admin, I want to seamlessly migrate report templates, connectors, and queries from one environment to another (e.g., Dev → UAT → Production) with clear feedback and zero surprises.

---

## Story 1 — Export a Template Package

**As a** Report Administrator on the **source** environment,  
**I want to** export a template as a self-contained package (JSON file),  
**So that** I can hand it off to another team or environment without any manual data collection.

### Acceptance Criteria
- [ ] An **Export** button is visible on the Template Details page.  
- [ ] Clicking Export immediately downloads a `.json` file.  
- [ ] The JSON file contains:
  - Template name and description.
  - All version files (Base64 encoded).
  - All mapped queries (with SQL text).
  - All connected Connectors (with URL, username — **no password**).
- [ ] After export, the UI shows a success toast: *"Template package exported successfully."*

---

## Story 2 — Upload the Exported JSON on the Target Environment

**As a** Report Administrator on the **target** environment,  
**I want to** upload the exported JSON file through the Import wizard,  
**So that** the system can analyze what is inside and guide me step by step.

### Acceptance Criteria
- [ ] An **Import Package** button is visible on the Templates list page.
- [ ] Clicking the button opens a **Step-by-step migration wizard** (not just a raw form).
- [ ] Step 1 is a file picker. Only `.json` files are accepted.
- [ ] If the JSON is invalid or malformed, the system immediately shows an error: *"Invalid migration package. Please export again."*
- [ ] **Upon successful JSON upload**, the system automatically parses it and advances to Step 2 — the user does **not** have to click anything extra.
- [ ] The wizard header always shows which file is being imported (filename).

---

## Story 3 — Smart Conflict Detection for Connectors

**As a** Report Administrator on the **target** environment,  
**I want the system to** detect whether a Connector from the export already exists in this environment,  
**So that** the system only shows the relevant action — no irrelevant options to confuse me.

### Acceptance Criteria

#### When the Connector **does NOT exist** in the target:
- [ ] No conflict prompt. System automatically sets action to **CREATE NEW**.
- [ ] User can optionally rename the Connector before import.
- [ ] Password field is shown and is **mandatory**.

#### When the Connector **already exists** (matched by name) in the target:
- [ ] System shows a **"⚠️ Already Exists"** badge next to the connector row.
- [ ] Two options are shown: **Use Existing** (Skip) | **Override Existing**.
- [ ] `CREATE_NEW` option is still available but labelled clearly: *"Import as New (rename required)"*.
- [ ] **Override Impact Panel** (visible before confirming): Shows a bullet list of all Queries and Report Templates that will be affected if this connector is overridden.
- [ ] System explicitly informs: *"Password was not exported. You must enter the connector password to proceed with Override or Create New."*
- [ ] **SKIP** (Use Existing) does NOT require a password — the existing connector credentials are used as-is.

---

## Story 4 — Smart Conflict Detection for Queries

**As a** Report Administrator,  
**I want the system to** detect whether each Query already exists in the target environment,  
**So that** I only override what needs to change, and I never accidentally delete working queries.

### Acceptance Criteria

#### When the Query **does NOT exist** in the target:
- [ ] Automatically set to **CREATE NEW**. No override option shown.
- [ ] User can optionally rename the query.

#### When the Query **already exists** in the target:
- [ ] System shows a **"⚠️ Already Exists"** badge next to the query row.
- [ ] Two options: **Use Existing** | **Override**.
- [ ] **Override Impact Panel**: Shows which Report Templates will be affected (since queries are linked to templates via mappings).
- [ ] If the user selects Override, a side-by-side **SQL diff view** is shown (Source SQL vs. Current SQL) so the user can see what will change before proceeding.

---

## Story 5 — Smart Conflict Detection for Report Template

**As a** Report Administrator,  
**I want the system to** detect if a template with the same name already exists on the target,  
**So that** I can decide to override it, skip it, or import it with a new name.

### Acceptance Criteria

#### When the Template **does NOT exist**:
- [ ] Automatically set to **CREATE NEW**.

#### When the Template **already exists**:
- [ ] System shows a **"⚠️ Already Exists"** badge on the template row.
- [ ] Options: **Use Existing (Attach Versions)** | **Override (Replace All Versions)**.
- [ ] A clear **red warning** is shown for Override: *"⚠️ Override will permanently delete all existing version snapshots of '[Template Name]' in this environment and replace them with the imported ones. This cannot be undone."*
- [ ] User must click a checkbox *"I understand, override anyway"* before the Execute button becomes active in Override mode.

---

## Story 6 — Impact Summary Before Final Execution

**As a** Report Administrator,  
**I want to** see a complete summary of every action the system is about to perform before I click Execute,  
**So that** I can review everything once and avoid surprises.

### Acceptance Criteria
- [ ] The final wizard step (Step 4) shows a **Migration Readiness Summary** card.
- [ ] Summary lists every entity with its chosen action:

  | Entity | Type | Action |
  | ------ | ---- | ------ |
  | DB_Prod | Connector | ❌ Override (will affect 4 queries, 2 templates) |
  | Report_Query_A | Query | ✅ Use Existing |
  | Monthly Report | Template | 🆕 Create New as "Monthly Report UAT" |

- [ ] Warnings and impacts are highlighted in red/amber.
- [ ] A **Back** button is present so the user can revise any decision.
- [ ] The **Execute Import** button is the only primary action at this step.

---

## Story 7 — Execute Import with Progress Feedback

**As a** Report Administrator,  
**I want** the import to run with live step-wise progress feedback,  
**So that** I know what is happening, especially if it takes a few seconds.

### Acceptance Criteria
- [ ] While importing, the button shows a loading spinner: *"Importing..."*
- [ ] All wizard inputs are disabled (non-editable) during execution to prevent duplicate submissions.
- [ ] On success: Show a **full-screen success banner** (not just a toast) listing what was created/overridden.
- [ ] On failure: Show the specific error (e.g., *"Could not override Connector 'DB_Prod': wrong password"*) and the user stays on the wizard to retry — wizard does NOT close.

---

## Story 8 — Post-Import Verification

**As a** Report Administrator,  
**After** a successful import,  
**I want** to quickly verify that everything landed correctly without hunting through menus.

### Acceptance Criteria
- [ ] After a successful import, the wizard success screen shows direct links:
  - *"View Imported Template →"* (takes user to the template details page)
  - *"View Connectors →"*
  - *"View Queries →"*
- [ ] The imported template appears at the **top** of the Templates list (sorted by latest created).
- [ ] The `createdBy` field on imported versions shows *"Migration System"* so it's auditable.

---

## Story 9 — Password Security Guardrails

**As a** Security-conscious Admin,  
**I want** the system to enforce password entry for any connector create/override operation,  
**So that** no connector is ever imported with an empty or insecure password.

### Acceptance Criteria
- [ ] If a Connector's action is **Override** or **Create New** and the password field is empty, the **Next** button on the Connector step is disabled with a tooltip: *"Enter password for all modified connectors to continue."*
- [ ] Password fields are always masked (`type="password"`).
- [ ] Password is **never stored** in the export JSON — only entered live during import.
- [ ] **SKIP** action for a connector does not require a password entry.

---

## Story 10 — Graceful Handling of Empty Exports

**As a** Report Administrator,  
**If** I import a template that has no connectors or queries (e.g., a static template),  
**I want** the wizard to skip the Connectors and Queries steps entirely,  
**So that** I don't see empty, confusing tables.

### Acceptance Criteria
- [ ] If the JSON has 0 connectors, the Connector step is automatically skipped in the stepper.
- [ ] If the JSON has 0 queries, the Query step is automatically skipped.
- [ ] The stepper shows only the steps that are relevant to the package content.

---

## UX Design Principles (Non-negotiable)

| Principle | Requirement |
| :--- | :--- |
| **No Silent Overrides** | Nothing is overwritten without the user explicitly choosing Override and seeing the impact. |
| **Contextual Options** | Override option ONLY appears when the entity already exists. Never shown for fresh imports. |
| **Impact Transparency** | Before overriding anything, the system must list affected downstream entities (queries → templates). |
| **Password Discipline** | Password fields appear only when needed. SKIP never needs a password. |
| **No Dead Ends** | Every error keeps the wizard open with a clear message — user never has to re-upload the file. |
| **Audit Trail** | All imported entities are tagged with `createdBy: "Migration System"` for traceability. |

---

## Gap Analysis vs. Current Implementation

> The following are features described in the user stories that are **not yet implemented** in the current code and represent the **target vision**.

| Gap | Current Behaviour | Expected Behaviour |
| :--- | :--- | :--- |
| Smart strategy defaulting | All connectors default to `SKIP` regardless | System checks if entity exists; defaults `OVERRIDE` if found, `CREATE_NEW` if not |
| Impact panel for connectors | Not shown | Show list of affected queries/templates before confirming override |
| SQL diff view for queries | Not shown | Side-by-side source vs. existing SQL before override |
| Override confirmation checkbox | Only a regular button | Red checkbox *"I understand"* before Execute for destructive actions |
| Post-import success links | Just a toast notification | Success screen with direct links to imported entities |
| Step skipping for empty sets | All 4 steps always shown | Connector/Query steps skipped if the export has none |
| Password validation gate | Password field exists but Next is not blocked | Next button disabled until mandatory passwords are filled |
