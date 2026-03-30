# Microservices Refactoring - Testing Guide

This guide provides step-by-step instructions to manually verify all the major enhancements introduced during the microservice refactoring. 

## Prerequisites
1. Ensure both microservices are running:
   - `report-service` should be accessible at **http://localhost:8084**
   - `connector-query-service` should be accessible at **http://localhost:8085**
2. Ensure the React frontend is running (`npm run dev`) and accessible at **http://localhost:5173**.

---

## Scenario 1: Vault Application-Level AES Encryption
**Objective:** Verify that connectors no longer store plaintext passwords in your actual SQL database and verify that Vault successfully handles cryptography.

1. Create a dummy connector:
   - **Frontend:** Navigate to the **Connectors** page, click "Add Connector". Fill out dummy database details (e.g. `jdbc:mysql://localhost/test`, `user`, password: `MySecretPassword123`).
2. Verify Database Stubs:
   - Connect to the `connector-query-service` H2 Database using your IDE or DBeaver.
   - Look at the `REPORT_CONNECTOR` table. You should **not** see a password column or any leaked passwords in the rows.
3. Validate Vault Encrypt/Decrypt APIs:
   - Use a tool like Postman to call the Admin test endpoints on `connector-query-service` (Port 8085).
   - **Encrypt Request:** `POST http://localhost:8085/api/vault/encrypt` (Body: `RawTextPassword`). Ensure the ciphertext is returned.
   - **Decrypt Request:** `POST http://localhost:8085/api/vault/decrypt` (Body: `<CipherTextFromPreviousStep>`). Ensure the text decrypted perfectly matches the raw payload.

---

## Scenario 2: Dependency Locks / Constraints
**Objective:** Verify that you cannot delete Connectors or Queries if they are currently mapped or in use.

### Test 2.1: Connector Deletion Lock
1. Navigate to **Queries** and create a new Query mapped to a Connector (e.g., `Sales DB`).
2. Navigate to **Connectors** and attempt to delete the `Sales DB` connector. 
3. **Expected Result:** The UI should block the deletion and display an error stating the connector is in use.

### Test 2.2: Query Deletion Lock
1. Navigate to **Templates** and upload a dummy Word/Excel file.
2. Click **+ Add** to map a Query to this Template. Let's map `Query1` to jsonNode `sales`.
3. Navigate to **Queries** and attempt to delete `Query1`.
4. **Expected Result:** The UI should block the deletion and display an error explaining that the query is currently mapped to a report template.

---

## Scenario 3: UI Full CRUD (Updates & Re-mappings)
**Objective:** Ensure complete editability across objects without losing data context or having to delete and recreate.

### Test 3.1: Update Connector Password
1. In the **Connectors** view, click the new **Update Password** button on an existing connector.
2. Provide a new password and save.
3. Verify that the system registers a "Password updated" success message without needing to adjust the JDBC URL or username.

### Test 3.2: Fully Editing Queries
1. In the **Queries** view, click **Edit** on an existing query. 
2. Change the SQL text (e.g., add `LIMIT 10`) and update the description.
3. Save the query, and notice that a `PUT` request updates the query directly instead of duplicating it.

### Test 3.3: Replacing Template Files in-place
1. In the **Templates** view, imagine you have a layout mistake in your Word Document (`.docx`). 
2. Instead of deleting the entire template (and losing all query mappings), click **Replace File**.
3. Upload a new `docx` file. 
4. Verify you get a success message, and notice that your mappings are **still intact**.

### Test 3.4: Overwriting Mapping Targets
1. In **Templates**, click on the small badge representing an existing query mapping (e.g. `sales ➜ Query1`).
2. The modal should open showing **Edit Mapping**.
3. Change the underlying Query from `Query1` to a completely different query, or change the `JSON Node Name`.
4. Click **Save Mapping**.
5. Check that the template now represents the new updated target query logic.

---

## Scenario 4: E2E Generation Over The Network Boundary
**Objective:** Ensure that the final combined architecture builds reports seamlessly across 8084 & 8085.

1. Navigate to the **Generate Report** page in the UI.
2. Select your template.
3. Choose the output format (`PDF`, `DOCX`, or `XLSX`).
4. Click **Generate & Download**.
5. **Expected Sequence Details Unseen:**
   - The UI pings `report-service` (8084).
   - `report-service` sends REST queries via `RestTemplate`/`RestClient` to `connector-query-service` (8085) for every mapped query simultaneously.
   - `connector-query-service` accesses the Vault, decrypts passwords, hits the physical databases, extracts Maps, and ships them back over the REST network.
   - `report-service` feeds the aggregate dataset directly into Aspose dynamically applying your chosen Cloud or Local `StorageStrategy`.
   - The UI initiates the file download to your machine. 
   - Note: Total generation process should remain highly responsive.
