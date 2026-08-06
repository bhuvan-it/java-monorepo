# Acme Java Monorepo (Nx + Maven Integration)

This repository implements a production-ready Java 21 monorepo architecture where **Nx owns the task graph and computation caching** and **Maven owns the compilation and build lifecycle**.

> [!WARNING]
> The experimental `@nx/maven` plugin was explicitly avoided because it is not production-ready. Instead, this monorepo uses a lightweight local Nx inference plugin (`tools/nx-maven/index.js`) that wraps standard `mvn` invocations in Nx targets.

---

## Architectural Principles

1. **Nx Owns Graph & Caching**: Nx constructs the build graph by parsing POM dependencies and calculates hashes across task inputs and outputs.
2. **Maven Owns Lifecycle**: Compilation, test execution (Surefire/Failsafe), code style (Spotless), coverage (JaCoCo), and packaging are performed natively by standard Maven plugins.
3. **No Reactor Coupling**: Nx builds one module at a time (`./mvnw install -DskipTests -f <path>/pom.xml`) in topological order. Nx never delegates build order determination to the Maven reactor.
4. **No Lock-in**: The root `pom.xml` retains its `<modules>` aggregator list. Deleting all Nx files (`nx.json`, `package.json`, `tools/`) leaves a 100% standard Maven multi-module project where `./mvnw verify` at the root builds the entire repository seamlessly.

---

## Module Dependency Graph

```
                   monorepo-parent (root POM)
                         /       \
                        /         \
                 common-core       \
                /     |            \
               /      |             \
   common-logging  domain-model     |
         \            |             /
          \       persistence      /
           \       /        \     /
           order-service   inventory-service
```

- `common-core`: Base domain primitives (e.g., `Money` value object). No internal dependencies.
- `common-logging`: Logging utility (`StructuredLogger`). Depends on `common-core`.
- `domain-model`: Order domain aggregate (`Order`, `OrderLine`). Depends on `common-core`.
- `persistence`: In-memory repository (`InMemoryOrderRepository`). Depends on `domain-model` (and transitively `common-core`). Uses `spring-context`.
- `order-service`: Spring Boot Web application. Depends on `persistence` and `common-logging`.
- `inventory-service`: Spring Boot Web application. Depends on `domain-model` and `common-logging`.

---

## Local Maven Repository Rationale & Caveat

### The Caching Problem
Standard Maven writes compiled JAR artifacts to `~/.m2/repository`. If Nx restores a cached build for `common-core`, its target directory (`libs/common-core/target`) is restored, but the JAR file in `~/.m2/repository` would be missing. Subsequent Maven invocations for downstream modules (`domain-model`) would fail due to missing local dependencies.

### The Solution
We relocate Maven's local repository inside the workspace:
- `.mvn/maven.config` defines `-Dmaven.repo.local=.m2/repository`.
- Each Nx module build target defines `outputs`:
  ```json
  [
    "{projectRoot}/target",
    "{workspaceRoot}/.m2/repository/com/acme/<artifactId>"
  ]
  ```
When Nx caches or restores a build, it caches and restores both the module's `target/` directory AND its installed artifacts in `.m2/repository/com/acme/<artifactId>`.

> [!IMPORTANT]
> **Caveat**: Because `-Dmaven.repo.local=.m2/repository` uses a relative path, `./mvnw` commands MUST be executed from the repository root (e.g. `./mvnw -f libs/common-core/pom.xml ...`). Nx targets automatically run from the workspace root (`cwd: "."`). Running raw `./mvnw` from inside a subdirectory would resolve `.m2/repository` relative to that subdirectory.

---

## Plugin Version Verification

> [!WARNING]
> All Maven plugin and dependency versions in `pom.xml` are pinned via properties (e.g., `<spotless.version>`, `<jacoco.version>`, `<spring.boot.version>`). Before deploying or using in production, verify these versions against Maven Central for security updates and compatibility.

---

## Common Commands

### Nx Commands

- **List all projects**:
  ```bash
  npx nx show projects
  ```

- **Build a specific app (triggers topological dependencies automatically)**:
  ```bash
  npx nx build order-service
  ```

- **Run tests across all projects**:
  ```bash
  npx nx run-many -t test
  ```

- **Run linting (Spotless check) across all projects**:
  ```bash
  npx nx run-many -t lint
  ```

- **Apply code formatting (Spotless apply)**:
  ```bash
  npx nx run-many -t format
  ```

- **Run affected tasks (e.g., in CI)**:
  ```bash
  npx nx affected -t lint test build --parallel=3
  ```

- **Visualize dependency graph**:
  ```bash
  npx nx graph
  ```

- **Serve a Spring Boot app**:
  ```bash
  npx nx serve order-service
  ```

### Standard Maven Fallback (Nx Bypassed)

From the repository root:
```bash
./mvnw verify
```

---

## How to Add a New Module

1. **Create module directory**: e.g., `libs/my-new-lib`.
2. **Add `pom.xml`**: Inherit from `com.acme:monorepo-parent` and set `<artifactId>my-new-lib</artifactId>`.
3. **Register in root POM**: Add `<module>libs/my-new-lib</module>` under `<modules>` in `pom.xml`.
4. **Register in dependencyManagement (if shared)**: Add `<dependency>` under `<dependencyManagement>` in `pom.xml`.
5. **Zero extra Nx configuration required!** The Nx inference plugin (`tools/nx-maven/index.js`) automatically globs for new `pom.xml` files, registers the new project, and infers target dependencies.
