# Master Prompt: Convert Any Java Repository to a Production-Grade Nx + Maven Monorepo

Copy and paste the prompt below into an AI coding agent to execute a complete, zero-defect migration of any Java/Maven repository into this Nx + Maven monorepo architecture.

---

```markdown
# System Directive: Transform Repository into an Nx + Maven Monorepo

Act as a Principal Software Engineer and Architect. Your task is to transform this Java repository (or create a new multi-module project) into a production-grade **Nx + Maven Monorepo** following a strict hybrid architecture.

- **Nx** owns the task dependency graph, computation caching, parallel execution, and `nx affected` filtering.
- **Maven** owns compilation, testing, code formatting, code coverage, and JAR packaging.
- **Local Plugin**: A lightweight plugin at `tools/nx-maven/index.js` must infer the workspace graph automatically from `pom.xml` files without relying on external plugins or manual Nx target declarations in every folder.

Do NOT delegate task execution to Maven reactor builds (do NOT run `mvn install` at the root for individual task execution). Each task must execute locally per project module using `mvn -f <path>/pom.xml` so Nx can cache outputs granularly.

Follow all instructions, architecture constraints, quality gates, and verification protocols below without deviation.

---

## 1. Local Maven Repository Relocation & Wrapper Setup

1. **Maven Wrapper**: Ensure Maven Wrapper (`./mvnw` and `mvnw.cmd`) is present in the workspace root using Maven 3.9.9 (`only-script` distribution). All commands executed by developers, CI, and the Nx plugin MUST shell `./mvnw` (or `mvnw.cmd` on Windows).
2. **Local Repository Relocation**: Create `.mvn/maven.config` in the workspace root containing:
   ```
   -Dmaven.repo.local=.m2/repository
   ```
   This isolates Maven dependencies inside the workspace at `.m2/repository`, allowing Nx to cache installed module JARs.

---

## 2. Nx Plugin Implementation (`tools/nx-maven/index.js` & `index.test.js`)

Create a local Nx plugin at `tools/nx-maven/index.js` that implements `createNodesV2` and `createDependencies`:

### Graph Parsing & Target Inference (`index.js`)
- **Header Parsing**: Parse POM header slices safely by stripping comments, `<dependencies>`, `<dependencyManagement>`, `<build>`, and `<profiles>` blocks before regex matching `<artifactId>`, `<packaging>`, `<description>`, and `<parent>`.
- **Target Generation Branching**:
  - **Root POM (`projectRoot === '.'`)**: Generate `build` (`./mvnw install -N -f pom.xml`), `lint` (`./mvnw spotless:check -N -f pom.xml`), and `format` (`./mvnw spotless:apply -N -f pom.xml`). Do NOT generate `test` or `verify` targets for the root POM.
  - **Sub-module POM (`packaging === 'pom'`)**: Generate `build` (`./mvnw install -DskipTests -f <path>`), `verify` (`./mvnw verify -f <path>` outputting `target/site/jacoco-aggregate`), `lint`, and `format`. Do **NOT** generate a `test` target for POM modules (preventing empty test runs on sourceless aggregators).
  - **JAR / WAR / Spring Boot Modules**: Generate `build` (`./mvnw install -DskipTests -f <path>`), `test` (`./mvnw test -f <path>`), `verify` (`./mvnw verify -f <path>`), `lint`, `format`, and `serve` (`./mvnw spring-boot:run -f <path>` if `spring-boot-maven-plugin` is present).
- **Target Outputs**:
  - `build`: `{projectRoot}/target` and `{workspaceRoot}/.m2/repository/<groupIdPath>/<artifactId>`.
  - `test`: `{projectRoot}/target/surefire-reports`, `{projectRoot}/target/site/jacoco`, `{projectRoot}/target/jacoco.exec`.
  - `verify`: `{projectRoot}/target/surefire-reports`, `{projectRoot}/target/failsafe-reports`, `{projectRoot}/target/site/jacoco`, `{projectRoot}/target/site/jacoco-aggregate`, `{projectRoot}/target/jacoco.exec`.
- **Metadata**:
  - `projectType`: `'application'` if Spring Boot app, `'library'` for all other modules (including POM aggregators).
  - `tags`: `['lang:java', 'packaging:<p>', 'type:<app|parent|lib>']`.
- **Dependency Inference**: `createDependencies` must iterate over `context.projects` to construct graph edges matching inter-module POM `<dependencies>` and `<parent>` references. Handle `${project.groupId}` and `${pom.groupId}` placeholders cleanly. Ignore dependencies inside `<dependencyManagement>`.

### Plugin Unit Tests (`tools/nx-maven/index.test.js`)
Create a comprehensive test suite using `node:test` covering:
1. `<dependencies>` placed before `<artifactId>`.
2. Omitted `<groupId>` inheriting parent groupId.
3. `${project.groupId}` and `${pom.groupId}` dependency resolution.
4. `<dependencyManagement>`-only dependencies producing zero graph edges.
5. Plugin coordinates in `<build>` producing zero graph edges.
6. Root POM aggregator producing `type:parent` tag and no `test` target.
7. Spring Boot POM producing `projectType: application` and `serve` target.
8. `<packaging>war</packaging>` receiving `packaging:war` tag.
9. Missing `<artifactId>` defaulting gracefully without crashing.
10. `createDependencies` using `context.projects` to build graph.
11. Non-root POM module (`<packaging>pom</packaging>`) producing `type:parent` tag, retaining `verify` target, and omitting `test` target.

---

## 3. Workspace Configuration (`nx.json`, `.nxignore`, `package.json`)

### `nx.json`
Configure `namedInputs` (`production`, `default`), `targetDefaults` (`build` outputs, `test` reports, `verify` outputs), and plugins (`tools/nx-maven/index.js`). Pinned `defaultBase: "origin/main"`.

### `.nxignore`
Create `.nxignore` in workspace root listing documentation and metadata files so documentation changes do not mark all workspace projects affected in CI:
```gitignore
*.md
docs/
LICENSE
CONTRIBUTING.md
.github/dependabot.yml
.github/CODEOWNERS
.github/ISSUE_TEMPLATE/
```
*(Leave `.github/workflows/` unignored so CI pipeline changes trigger verification runs).*

---

## 4. Root Parent POM (`pom.xml`) Configuration

Configure the parent `pom.xml` with standard quality tooling:

1. **Properties**:
   - Set `<java.version>21</java.version>` (or target JDK) and `<maven.compiler.release>21</maven.compiler.release>`.
   - Set JaCoCo line coverage minimum threshold `<jacoco.line.minimum>0.70</jacoco.line.minimum>`.
2. **Enforcer Plugin (`maven-enforcer-plugin`)**:
   - Enforce Maven version `[3.9.0,)`.
   - Enforce Java version dynamically: `<requireJavaVersion><version>[${java.version},)</version></requireJavaVersion>`.
   - Enforce `<banDuplicatePomDependencyVersions/>`.
3. **Spotless Code Formatter (`spotless-maven-plugin`)**:
   - Configure `palantirJavaFormat`.
   - Configure `<removeUnusedImports/>` and `<importOrder/>`.
   - Configure `<trimTrailingWhitespace/>` and `<endWithNewline/>`.
4. **JaCoCo Coverage (`jacoco-maven-plugin`)**:
   - Bind `prepare-agent` to `test` phase.
   - Bind `report` to `test` phase.
   - Bind `check` to `verify` phase enforcing `${jacoco.line.minimum}`.
5. **Surefire & Failsafe Plugins**:
   - `maven-surefire-plugin`: Include `**/*Test.java`, exclude `**/*IT.java`, set `<argLine>@{argLine}</argLine>`.
   - `maven-failsafe-plugin`: Include `**/*IT.java`, bind `integration-test` and `verify` goals.
6. **CycloneDX SBOM (`cyclonedx-maven-plugin`)**:
   - Bind `makeAggregateBom` to `package` phase, generating `bom.json` and `bom.xml`.

---

## 5. Spring Boot Application Packaging & Configuration

For any Spring Boot application modules (`apps/*`):

1. **Executable Classifier**: Configure `spring-boot-maven-plugin` with `<classifier>exec</classifier>` in `pom.xml`:
   ```xml
   <plugin>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-maven-plugin</artifactId>
     <configuration>
       <classifier>exec</classifier>
     </configuration>
   </plugin>
   ```
   *Rationale*: This ensures the module generates both a standard library JAR (`app-1.0.0.jar`) for compile-time/coverage aggregation dependencies and an executable archive (`app-1.0.0-exec.jar`) for execution.
2. **Port Management**: Ensure each Spring Boot app defines a distinct non-conflicting `server.port` in `src/main/resources/application.yaml` (e.g. `8081`, `8082`).
3. **Actuator & Observability**: Include `spring-boot-starter-actuator` and expose `/actuator/health` and `/actuator/info` endpoints. Remove hand-rolled status endpoints.

---

## 6. Aggregate Coverage Module (`libs/coverage-aggregate`)

Create an aggregate coverage module at `libs/coverage-aggregate`:
1. `<packaging>pom</packaging>`.
2. Include `<dependency>` declarations for all code modules in the workspace.
3. Configure `jacoco-maven-plugin` to execute `report-aggregate` during the `verify` phase.

---

## 7. Application & Domain Code Architecture

1. **Domain Error Representation**: Represent business operations returning domain errors using a sealed `Result<T>` type (`Result.Ok<T>` / `Result.Err<T>`). Never use runtime exceptions for expected domain validation failures.
2. **Controller Error Handling**: Map `Result.Err` in Spring `@RestController` classes to **HTTP 400 Bad Request** with structured JSON payloads (`{"error": "message"}`). Include `@ControllerAdvice` (`GlobalExceptionHandler`) for unhandled exceptions.
3. **Dependency Injection**: Register shared services (e.g., `AuditLog`) as Spring `@Bean` components reading configuration (e.g., `${spring.application.name}`). Use single-constructor injection across all services.
4. **Thread Safety**: Use `ConcurrentHashMap` with defensive copying on read in any in-memory repositories.

---

## 8. Test Quality & Conventions

1. **Assertion Library**: Standardize 100% of unit and integration tests on **AssertJ** (`assertThat`). Do NOT mix with JUnit 5 `assertEquals`.
2. **Zero Wildcard Imports**: Wildcard static imports (`import static ...*;`) are strictly prohibited. Use explicit single-symbol static imports.
3. **Controller Unit Tests**: Use `@WebMvcTest` with `MockMvc` to test request parameter binding, service delegation, 200 OK responses, and 400 Bad Request error paths.
4. **Integration Tests**: Name end-to-end IT files `*IT.java`. Annotate with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and execute using `TestRestTemplate`.

---

## 9. Supply-Chain Hardening & CI Workflow (`.github/workflows/ci.yml`)

Create `.github/workflows/ci.yml`:
1. **Pinned Action SHAs**: Pin ALL 7 GitHub Actions (`actions/checkout`, `nrwl/nx-set-shas`, `actions/setup-java`, `actions/setup-node`, `actions/cache` [x2], `actions/upload-artifact`) to 40-character commit SHAs with inline version comments.
2. **Security & Governance**: Set `permissions: contents: read`, `concurrency`, and `timeout-minutes: 30`.
3. **Pipeline Steps**:
   - `npm run test:plugin` (Runs local plugin unit tests).
   - `npx nx affected -t lint test build` (Runs affected code tasks in parallel).
   - `npx nx affected -t verify` (Runs integration tests and JaCoCo coverage gates).
   - `./mvnw -B verify` (Plain-Maven escape hatch test on `main` branch).
4. **Dependabot Configuration**: Create `.github/dependabot.yml` configured for weekly `maven` and `github-actions` updates.

---

## 10. Verification Protocol

After completing the implementation, run the following verification protocol to guarantee 100% correctness:

1. **Plugin Unit Tests**:
   ```bash
   node --test tools/nx-maven/
   # Must pass 11/11 tests
   ```
2. **Workspace Graph & Project Inspection**:
   ```bash
   npx nx show projects
   npx nx show project coverage-aggregate
   # Verify coverage-aggregate targets: build, verify, lint, format (no test target)
   ```
3. **Nx Caching & Warm Build Test**:
   ```bash
   npx nx run-many -t build && npx nx run-many -t build
   # Second run MUST achieve 100% cache hits (8/8 tasks)
   ```
4. **Local Repository Cache Restoration Test**:
   ```bash
   rm -rf .m2 libs/*/target apps/*/target
   npx nx run-many -t build
   find .m2/repository -name "*.jar"
   # Must restore all module JARs to .m2 from cache
   ```
5. **Full Plain-Maven Verification**:
   ```bash
   ./mvnw -B verify
   # Must return 100% BUILD SUCCESS across all reactor modules
   ```
6. **Aggregate Coverage Report Verification**:
   ```bash
   ./mvnw -B -f libs/coverage-aggregate/pom.xml verify
   ls libs/coverage-aggregate/target/site/jacoco-aggregate/index.html
   # Must exist and contain multi-module coverage analysis
   ```
7. **Wildcard Static Import Audit**:
   ```bash
   grep -rn "import static .*\*;" libs apps --include=*.java
   # Must return 0 matches
   ```
8. **Pinned GitHub Actions Audit**:
   ```bash
   grep -o "uses: .*@[0-9a-f]\{40\}" .github/workflows/ci.yml | wc -l
   # Must equal total action step count (7)
   ```
```
