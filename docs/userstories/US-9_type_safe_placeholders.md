# User Story: Type-Safe Query Parameters & Validation

## Context
As a Developer integrating the Reporting Module into other applications, I want the system to provide metadata about placeholder types (e.g., DATE, NUMBER, STRING) and validate incoming data against these types. This ensures that the calling module can render the correct UI and avoids runtime query execution errors due to data type mismatches.

## Acceptance Criteria

### 1. Placeholder Metadata Definition
- When creating/editing a Query, the user should be able to define the **Data Type** for each discovered placeholder.
- Supported types: `STRING`, `NUMBER`, `DATE`, `BOOLEAN`.
- Defaults to `STRING` if not specified.

### 2. Structured Metadata API
- The existing placeholders API (`/api/queries/{id}/placeholders`) should be updated or a new endpoint created to return structured objects:
  ```json
  [
    { "name": "startDate", "type": "DATE", "description": "The first day of the report" },
    { "name": "minAmount", "type": "NUMBER", "description": "Minimum transaction value" }
  ]
  ```

### 3. Server-Side Validation
- Before executing a query via the report generation API, the system must validate the provided parameters against the defined metadata.
- If a parameter type does not match (e.g., a "Hello" string sent for a `DATE` placeholder), the API must return a `400 Bad Request`.
- The error message should be descriptive: `"Invalid value for placeholder 'startDate': Expected DATE format (YYYY-MM-DD), but received 'Hello'."`

### 4. Integration Friendly
- The metadata enables 3rd-party modules to dynamically generate forms with appropriate input components (DatePicker, NumberInput, etc.).

## Technical Tasks
- [ ] Add `placeholderMetadata` field to `ReportQuery` entity (JSON or separate table).
- [ ] Update `ReportQueryDto` to carry metadata.
- [ ] Implement `PlaceholderValidator` utility in the backend.
- [ ] Integrate the validator into `DatabaseExecutionService`.
- [ ] Update frontend Query Editor to allow setting types.
