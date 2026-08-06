# Contributing Guidelines

Thank you for contributing to this Nx + Maven Java monorepo! Please follow these standards to ensure code quality and consistency across the repository.

---

## 1. Development Prerequisites & Setup

- **Java JDK**: Version 21 or higher.
- **Node.js**: Version 22 or higher.
- **Maven**: Use the root wrapper `./mvnw` (Maven 3.9.9 pinned).

---

## 2. Code Quality & Conventions

### Java Style & Formatting
- **Formatter**: Code formatting is strictly enforced by **Spotless** (`palantirJavaFormat`). Run `./mvnw spotless:apply` or `npx nx run-many -t format` to format your changes.
- **Imports**: Wildcard imports (`import static ...*;`) are **prohibited**. Always use explicit single-type imports.

### Testing Conventions
- **Assertion Library**: Use **AssertJ** (`org.assertj.core.api.Assertions.assertThat`) for assertions across all unit and integration tests.
- **Controller Unit Tests**: Use `@WebMvcTest` with `MockMvc` to test controller routing, request parameter binding, and HTTP status responses (including 400 Bad Request paths).
- **Integration Tests**: Name end-to-end IT files `*IT.java`. Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`.

---

## 3. Verification Protocol

Before submitting a Pull Request, run the full verification protocol locally:

```bash
# 1. Plugin unit tests
node --test tools/nx-maven/

# 2. Build & Nx Caching Check
npx nx run-many -t build

# 3. Full Maven Lifecycle & Coverage Enforcement
./mvnw -B verify
```

All Pull Requests must pass CI checks in `.github/workflows/ci.yml`.
