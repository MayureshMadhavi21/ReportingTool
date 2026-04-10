# User Story: Context Path Configuration for Microservices

## ID: US-11
## Title: Context Path Configuration for Microservices

### Description
As a system administrator, I want to have unique context paths for each microservice so that I can route traffic to them through a single reverse proxy or API gateway more easily.

### Acceptance Criteria
1. The `report-service` should be accessible via the `/report-service` context path.
2. The `connector-query-service` should be accessible via the `/connector-service` context path.
3. The frontend application should be updated to use these new context paths for all API calls.
4. Swagger documentation for each service should be accessible under their respective context paths.

### Technical Notes
- Implement `server.servlet.context-path` in `application.yml` for both Spring Boot services.
- Update `baseURL` configuration in the frontend API client (Axios).

### Impact Analysis
- All external calls to the backend services must now include the context path in the URL.
- No changes required to the internal business logic or database schema.
