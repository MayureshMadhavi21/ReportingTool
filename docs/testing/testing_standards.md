# Spring Boot Testing Standards & Best Practices

This document outlines the mandatory testing standards for the Project. Adherence to these standards ensures a fast, reliable, and maintainable test suite.

## 1. Folder Structure
Tests must strictly mirror the package structure of the `src/main/java` directory.

```text
src/test/java/com/report/backend/
├── controller/          # Controller tests (@WebMvcTest)
├── service/             # Service tests (@ExtendWith)
└── util/                # Test utilities (TestDataFactory)
```

## 2. Naming Conventions

| Component | Convention | Example |
| :--- | :--- | :--- |
| **Class Name** | `[ClassName]Test` | `ReportQueryServiceTest` |
| **Method Name** | `[Method]_[Scenario]_[Expected]` | `createConnector_DuplicateName_ThrowsException` |
| **Mock Variables**| `[FieldName]Mock` | `repositoryMock` |

## 3. Mandatory Frameworks & Mocking Rules

- **Unit Testing**: JUnit 5 (Jupyter)
- **Mocking**: Mockito Framework
- **Isolation**: 
    - **No Real Database**: Never connect to a physical or in-memory DB (H2).
    - **No TestContainers**: Tests must run without Docker or external dependencies.
    - **No Spring Context in Services**: Use `@ExtendWith(MockitoExtension.class)` for services to keep tests millisecond-fast.
    - **Slicing**: Use `@WebMvcTest` for controllers to only load the web layer.

## 4. Best Practices (The "Senior" Way)

### A. Use `TestDataFactory`
Never instantiate entities/DTOs directly in test methods. Use a centralized factory to maintain DRY.
```java
ReportConnectorDto dto = TestDataFactory.createConnectorDto();
```

### B. `ArgumentCaptor` for Verification
Use `ArgumentCaptor` to verify that complex objects passed to mocks have the expected state.
```java
ArgumentCaptor<ReportConnector> captor = ArgumentCaptor.forClass(ReportConnector.class);
verify(repository).save(captor.capture());
assertEquals("Expected Name", captor.getValue().getName());
```

### C. Assertions
Use `assertThrows` for exception handling instead of `@Test(expected = ...)`.
```java
assertThrows(RuntimeException.class, () -> service.getById("999"));
```

## 5. Common Mistakes to Avoid

1. **`@SpringBootTest` Overuse**: This loads the full application context and slows down the build significantly. Avoid for Unit Tests.
2. **Hardcoded IDs**: Use UUIDs or constants from `TestDataFactory`.
3. **Missing `verify()`**: Always verify that important mock methods (like `save` or `delete`) were actually called.
4. **Leaky Tests**: Ensure mocks are reset or re-initialized between tests (handled automatically by `@ExtendWith(MockitoExtension.class)`).
