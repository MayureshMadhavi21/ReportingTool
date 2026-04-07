# User Stories: Senior Java Testing Suite

This document defines the requirements for each phase of the testing transformation.

---

## Story 1: Setup Testing Framework
**Narrative**: 
As a **DevOps/Lead Engineer**, 
I want to **Standardize the testing dependencies and folder structure**, 
So that **All developers follow a common architectural pattern for tests.**

### Acceptance Criteria
- [ ] `pom.xml` contains `spring-boot-starter-test`, `mockito-core`, and `junit-jupiter`.
- [ ] Folder structure mirrors `src/main/java`.
- [ ] Base test configuration using `@ExtendWith(MockitoExtension.class)` is defined.

### Technical Implementation Details
1. Update parent or service `pom.xml` to include canonical Spring Boot testing starters.
2. Initialize `src/test/java/com/report/backend` in each service.

### Example Code Snippet
```java
// Testing Framework Base
@ExtendWith(MockitoExtension.class)
abstract class BaseUnitTest { 
    // Shared mocks or logic
}
```

### Definition of Done
- Build succeeds with `mvn clean compile`.
- Test directories are consistent across all microservices.

---

## Story 2: Controller Layer Test Cases
**Narrative**: 
As a **Backend Developer**, 
I want to **Write tests for REST endpoints without loading the full context**, 
So that **API contracts and input validations are verified quickly.**

### Acceptance Criteria
- [ ] Use `@WebMvcTest` with targeted Controller class.
- [ ] Use `MockMvc` for HTTP calls (GET, POST, etc.).
- [ ] Mock all service dependencies used by the controller.
- [ ] Validate HTTP status, JSON payload, and error structures.

### Technical Implementation Details
1. Mock the specific `Service` implementation using `@MockBean`.
2. Use `MockMvcResultMatchers` for comprehensive assertions.

### Example Code Snippet
```java
@WebMvcTest(VaultController.class)
class VaultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VaultService vaultService;

    @Test
    void getPassword_ShouldReturn200() throws Exception {
        when(vaultService.getPassword("test")).thenReturn("secret");

        mockMvc.perform(get("/api/vault/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("secret"));
    }
}
```

### Definition of Done
- Controllers reach >90% code coverage.
- All HTTP response codes (Happy/Error) are verified.

---

## Story 3: Service Layer Test Cases
**Narrative**: 
As a **Lead Architect**, 
I want to **Verify business logic in isolation from databases and APIs**, 
So that **Tests are resilient and run in milliseconds.**

### Acceptance Criteria
- [ ] Use `@ExtendWith(MockitoExtension.class)`.
- [ ] Mock all `Repository` and external `Client` dependencies.
- [ ] Test complex business rules and DTO mappings.

### Technical Implementation Details
1. Use `@InjectMocks` to wire dependencies automatically.
2. Inject `TestDataFactory` for mock objects.

### Example Code Snippet
```java
@ExtendWith(MockitoExtension.class)
class ReportQueryServiceTest {

    @Mock
    private ReportQueryRepository repository;

    @InjectMocks
    private ReportQueryService service;

    @Test
    void findById_ShouldReturnDto() {
        ReportQuery entity = TestDataFactory.createEntity();
        when(repository.findById("1")).thenReturn(Optional.of(entity));

        ReportQueryDto result = service.getQueryById("1");

        assertNotNull(result);
        assertEquals(entity.getName(), result.getName());
    }
}
```

### Definition of Done
- Service logic, including transformations and validations, is fully tested.
- Zero reliance on physical DB or H2.

---

## Story 4: Mockito Mocking Strategy
**Narrative**: 
As a **Testing Expert**, 
I want to **Establish a unified mocking and verification strategy**, 
So that **Collaborative operations between layers are accurately simulated.**

### Acceptance Criteria
- [ ] Use `when(...).thenReturn(...)` for stubbing.
- [ ] Use `ArgumentCaptor` for verifying transformed entities prior to saving.
- [ ] Use `verify(mock, times(n))` to enforce operational contracts.

### Technical Implementation Details
1. Standardize use of `any()` or `eq()` for flexible yet precise matching.
2. Leverage `ReflectionTestUtils` only if private field overrides are mandatory.

### Example Code Snippet
```java
@Test
void save_ShouldCaptureMappedEntity() {
    service.save(dto);

    ArgumentCaptor<ReportConnector> captor = ArgumentCaptor.forClass(ReportConnector.class);
    verify(repository).save(captor.capture());
    
    assertEquals(dto.getJdbcUrl(), captor.getValue().getJdbcUrl());
}
```

### Definition of Done
- All interactions between Service and Repository are verified via `verify()`.

---

## Story 5: Exception Handling Test Cases
**Narrative**: 
As a **Quality Engineer**, 
I want to **Test all failure scenarios and custom error logic**, 
So that **The system fails gracefully and provides informative errors.**

### Acceptance Criteria
- [ ] Test `NotFoundException`, `ValidationException`, etc.
- [ ] Verify `assertThrows` and the error message content.

### Technical Implementation Details
1. Use JUnit 5's `assertThrows` to capture and inspect the actual exception object.

### Example Code Snippet
```java
@Test
void delete_MissingId_ThrowsNotFound() {
    when(repository.existsById("404")).thenReturn(false);

    RuntimeException ex = assertThrows(RuntimeException.class, 
        () -> service.delete("404"));
    
    assertTrue(ex.getMessage().contains("Not Found"));
}
```

### Definition of Done
- Every `throw` statement in the production code is exercised by at least one test.

---

## Story 6: JaCoCo Configuration
**Narrative**: 
As a **Build Engineer**, 
I want to **Configure JaCoCo for accurate coverage reporting**, 
So that **The team has visibility into testing gaps.**

### Acceptance Criteria
- [ ] Maven `jacoco-maven-plugin` is integrated.
- [ ] Exclusions set for DTOs, Entities, and Autogenerated code.
- [ ] HTML report generated after `mvn test`.

### Technical Implementation Details
1. Configure `prepare-agent` and `report` goals in `pom.xml`.
2. Add `<excludes>` list for non-logical classes.

### Example Code Snippet
```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
  <configuration>
    <excludes>
      <exclude>**/dto/**</exclude>
      <exclude>**/entity/**</exclude>
    </excludes>
  </configuration>
</plugin>
```

### Definition of Done
- Coverage report is visible at `target/site/jacoco/index.html`.

---

## Story 7: Achieving 80% Coverage Strategy
**Narrative**: 
As a **Product Owner**, 
I want to **Ensure the core system is reliable through high coverage**, 
So that **The probability of regression is minimized.**

### Acceptance Criteria
- [ ] Coverage includes 100% of the Service layer's conditional paths.
- [ ] Edge cases (null inputs, empty lists) are tested.

### Technical Implementation Details
1. Use parameterized tests (`@ParameterizedTest`) for testing multiple input variations.
2. Focus on "Boundary Testing" for numeric or logical limits.

### Example Code Snippet
```java
@ParameterizedTest
@ValueSource(strings = {"", " ", "null"})
void validate_InvalidInput_ReturnsError(String input) {
    // Logic to verify blank or null handling in service
}
```

### Definition of Done
- Minimum 80% coverage on targeted service/controller modules.

---

## Story 8: CI/CD Coverage Enforcement
**Narrative**: 
As a **Lead Architect**, 
I want to **Integrate coverage tracking into the build lifecycle**, 
So that **The team can monitor health trends.**

### Acceptance Criteria
- [ ] JaCoCo runs automatically in the CI pipeline.
- [ ] (Future) Build fails if coverage drops below baseline (currently report-only).

### Technical Implementation Details
1. Ensure the `test` phase of Maven is part of the central pipeline.

### Definition of Done
- Coverage metrics are accessible from the build artifacts of every PR.
