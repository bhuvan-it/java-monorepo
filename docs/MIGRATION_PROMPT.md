# Master Migration Blueprint: Convert Any Java Repository to a Production-Grade Nx + Maven Monorepo

Copy and paste the prompt below into an AI coding agent to execute a safe, phased, zero-defect migration of any existing Java/Maven repository into an Nx + Maven monorepo architecture.

---

```markdown
# System Directive: Transform Repository into an Nx + Maven Monorepo

Act as a Principal Software Engineer and Architect. Your task is to execute a safe, phased migration of this Java repository into a production-grade **Nx + Maven Monorepo** following a strict hybrid architecture.

- **Nx** owns the task dependency graph, computation caching, parallel execution, and `nx affected` filtering.
- **Maven** owns compilation, testing, code formatting, code coverage, dependency enforcement, and JAR packaging.
- **Local Plugin**: A lightweight plugin at `tools/nx-maven/index.js` infers the workspace graph automatically from `pom.xml` files without manual per-folder Nx targets.

Do NOT delegate task execution to Maven reactor builds (do NOT run `mvn install` at the root for individual task execution). Each task must execute locally per project module using `./mvnw -f <path>/pom.xml` so Nx can cache outputs granularly.

---

## Phase 0: Discovery, Baselines & Module Strategy (PRE-MIGRATION SAFEGUARDS)

Before modifying any source code or configuration files, execute discovery and record baselines:

1. **Inventory Current Workspace**:
   - Record JDK version, Maven version, packaging types, module layout, and existing CI workflows.
   - Identify existing `.mvn/` configurations, `settings.xml` mirrors, deployment pipelines, and `nexus-staging-maven-plugin` / `maven-deploy-plugin` steps.
2. **Coordinate Stability Guarantee**:
   - `groupId`, `artifactId`, and `version` coordinates of existing modules MUST remain 100% stable so downstream consumers are not broken.
3. **Module Decomposition Strategy**:
   - Do **NOT** attempt to split monolithic projects or alter existing module directory structures during the build migration phase.
   - Adopting Nx + Maven task caching works seamlessly on any directory layout. Module decomposition (e.g. splitting into `libs/` and `apps/`) is an independent refactoring concern and MUST be deferred to a separate phase *after* the build migration lands.
4. **Capture Baseline Artifacts**:
   Run the following baseline commands and save outputs:
   ```bash
   ./mvnw -B dependency:tree > /tmp/before-deps.txt
   ./mvnw -B test > /tmp/before-tests.txt
   ./mvnw -B verify > /tmp/before-verify.txt
   ```
   Extract and record current JaCoCo line coverage percentage and total test counts as your migration baseline.

---

## Phase 1: Local Maven Relocation & Wrapper Setup

1. **Maven Wrapper**: Ensure Maven Wrapper (`./mvnw` and `mvnw.cmd`) is present using Maven 3.9.9 (`only-script` distribution). All commands MUST shell `./mvnw` (or `mvnw.cmd`).
2. **Local Repository Relocation**: Create `.mvn/maven.config` containing:
   ```
   -Dmaven.repo.local=.m2/repository
   ```
   This isolates Maven dependencies inside `.m2/repository` in the workspace root, enabling Nx to cache built module JARs.
   *Note on Release Pipelines*: If existing deployment/release jobs (`mvn deploy`) expect installed artifacts in `$HOME/.m2/repository`, override `-Dmaven.repo.local=$HOME/.m2/repository` specifically in release pipeline scripts.
3. **Workspace `.gitignore`**: Ensure `.gitignore` includes:
   ```gitignore
   # Nx & Maven Build Artifacts
   .m2/
   .nx/
   target/
   **/target/
   dist/
   tmp/
   graph.json

   # IDE & OS
   .idea/
   *.iml
   .vscode/
   .DS_Store
   ```

---

## Phase 2: Nx Plugin Implementation (`tools/nx-maven/index.js` & `index.test.js`)

Create the Nx plugin at `tools/nx-maven/index.js` implementing `createNodesV2` and `createDependencies`:

### Header Slice & Coordinate Parsing (`index.js`)
To prevent child POMs from inheriting the parent's `artifactId`:
- **Own Coordinate Match**: Extract own coordinates by stripping comments and stripping `<parent>`, `<dependencies>`, `<dependencyManagement>`, `<build>`, `<profiles>`, and `<reporting>` blocks before matching `<artifactId>`, `<packaging>`, and `<description>`.
- **Parent Match**: Extract parent `artifactId` separately from the `<parent>` block before stripping it.

### Target Inference & Packaging Branching
- **Root POM (`projectRoot === '.'`)**: Generate `build` (`./mvnw install -N -f pom.xml`), `lint` (`./mvnw spotless:check -N -f pom.xml`), and `format` (`./mvnw spotless:apply -N -f pom.xml`). Do NOT generate `test` or `verify` targets for root.
- **Sub-module POM (`packaging === 'pom'`)**: Generate `build` (`./mvnw install -DskipTests -f <path>`), `verify` (`./mvnw verify -f <path>` outputting `target/site/jacoco-aggregate`), `lint`, and `format`. Do **NOT** generate a `test` target for POM modules (preventing empty test runs on sourceless aggregators).
- **JAR / WAR / Spring Boot Modules**: Generate `build` (`./mvnw install -DskipTests -f <path>`), `test` (`./mvnw test -f <path>`), `verify` (`./mvnw verify -f <path>`), `lint`, `format`, and `serve` (`./mvnw spring-boot:run -f <path>` if `spring-boot-maven-plugin` is present).

### Target Outputs & Metadata
- **`build` Outputs**: `{projectRoot}/target` and `{workspaceRoot}/.m2/repository/<groupIdPath>/<artifactId>`.
- **`test` Outputs**: `{projectRoot}/target/surefire-reports`, `{projectRoot}/target/site/jacoco`, `{projectRoot}/target/jacoco.exec`.
- **`verify` Outputs**: `{projectRoot}/target/surefire-reports`, `{projectRoot}/target/failsafe-reports`, `{projectRoot}/target/site/jacoco`, `{projectRoot}/target/site/jacoco-aggregate`, `{projectRoot}/target/jacoco.exec`.
- **`projectType` & `tags`**:
  - `projectType`: `'application'` if Spring Boot app, `'library'` for all other modules (including POM aggregators).
  - `tags`: `['lang:java', 'packaging:<p>', 'type:<app|parent|lib>']`.
- **Graph Dependencies (`createDependencies`)**: Iterate over `context.projects` to construct graph edges matching inter-module POM `<dependencies>` and `<parent>` references. Support `options.groupId` or `options.groupIds` array to handle multi-groupId enterprise repositories.

### Plugin Unit Tests (`tools/nx-maven/index.test.js`)
Create a unit test suite using `node:test` covering:
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

## Phase 3: Workspace Config (`nx.json`, `package.json`, `.nxignore`)

### 1. `nx.json` (Explicit Options Form & Task Inputs)
```json
{
  "$schema": "./node_modules/nx/schemas/nx-schema.json",
  "defaultBase": "origin/main",
  "namedInputs": {
    "default": ["{projectRoot}/**/*", "sharedGlobals"],
    "production": [
      "default",
      "!{projectRoot}/src/test/**/*",
      "!{projectRoot}/**/*.md",
      "!{projectRoot}/src/test/resources/**/*"
    ],
    "sharedGlobals": [
      "{workspaceRoot}/pom.xml",
      "{workspaceRoot}/.mvn/**/*",
      "{workspaceRoot}/tools/nx-maven/**/*"
    ]
  },
  "targetDefaults": {
    "build": {
      "cache": true,
      "dependsOn": ["^build"],
      "inputs": ["production", "^production"]
    },
    "test": {
      "cache": true,
      "dependsOn": ["build", "^build"],
      "inputs": ["default", "^production"]
    },
    "verify": {
      "cache": true,
      "dependsOn": ["build", "^build"],
      "inputs": ["default", "^production"]
    },
    "lint": {
      "cache": true,
      "inputs": ["default"]
    }
  },
  "plugins": [
    {
      "plugin": "./tools/nx-maven/index.js",
      "options": {
        "groupId": "com.acme",
        "groupIds": ["com.acme"]
      }
    }
  ]
}
```

### 2. `package.json`
Pin `nx` exactly matching the working repo version (`23.1.1`) and define workspace scripts:
```json
{
  "name": "monorepo-root",
  "version": "1.0.0-SNAPSHOT",
  "private": true,
  "scripts": {
    "nx": "nx",
    "build": "nx run-many -t build",
    "test": "nx run-many -t test",
    "verify": "nx run-many -t verify",
    "lint": "nx run-many -t lint",
    "format": "nx run-many -t format",
    "test:plugin": "node --test tools/nx-maven/"
  },
  "devDependencies": {
    "nx": "23.1.1"
  }
}
```

### 3. `.nxignore` (Narrowed Metadata Filtering)
Create `.nxignore` to prevent documentation/metadata edits from causing workspace-wide affected runs:
```gitignore
*.md
docs/
LICENSE
CONTRIBUTING.md
.github/dependabot.yml
.github/CODEOWNERS
.github/ISSUE_TEMPLATE/
```
*(Leave `.github/workflows/` unignored so CI workflow edits trigger verification runs).*

---

## Phase 4: Parent POM (`pom.xml`) & Quality Gates (Migration Mode)

Configure parent `pom.xml` with non-destructive, ratcheted quality gates:

1. **Enforcer Plugin (`maven-enforcer-plugin`)**:
   - Dynamic Java version: `<requireJavaVersion><version>[${java.version},)</version></requireJavaVersion>`.
   - `<banDuplicatePomDependencyVersions/>`.
2. **Dependency Hygiene (`maven-dependency-plugin`)**:
   - Bind `analyze-only` to `verify` phase. Set `<failOnWarning>false</failOnWarning>` initially during migration. Once warnings are cleaned, flip to `<failOnWarning>true</failOnWarning>`.
3. **Spotless Code Formatter (`spotless-maven-plugin`)**:
   - Configure `palantirJavaFormat`, `removeUnusedImports`, `importOrder`, `trimTrailingWhitespace`, `endWithNewline`.
   - Use `<ratchetFrom>origin/main</ratchetFrom>` so formatting rules only apply to modified files and do not disrupt `git blame` across untouched legacy code.
4. **JaCoCo Coverage Ratcheting (`jacoco-maven-plugin`)**:
   - Bind `prepare-agent` and `report` to `test` phase, and `check` to `verify` phase.
   - Set `<jacoco.line.minimum>` to the **measured baseline coverage percentage from Phase 0**, ratcheting up over time.
5. **Surefire & Failsafe Plugins**:
   - `maven-surefire-plugin`: Include `**/*Test.java`, exclude `**/*IT.java`, set `<argLine>@{argLine}</argLine>`.
   - `maven-failsafe-plugin`: Include `**/*IT.java`, bind `integration-test` and `verify` goals.
6. **CycloneDX SBOM (`cyclonedx-maven-plugin`)**:
   - Bind `makeAggregateBom` to `package` phase, producing `bom.json` and `bom.xml`.

---

## Phase 5: Spring Boot & Aggregate Coverage

1. **Spring Boot Exec Classifier**: Configure `spring-boot-maven-plugin` with `<classifier>exec</classifier>` in application POMs so standard library JARs remain available for compile-time/JaCoCo dependencies alongside executable archives.
2. **Aggregate Coverage Module**: Create an aggregate coverage module (`<packaging>pom</packaging>`) depending on all code modules and running `jacoco:report-aggregate` on `verify`.

---

## Phase 6: Supply-Chain Hardening & CI Workflow (`.github/workflows/ci.yml`)

1. **Action SHA Pinning & Security**: Pin all GitHub Actions to 40-character commit SHAs. Include `permissions: contents: read`, `concurrency`, and `timeout-minutes: 30`.
2. **CI Caching & Pipeline Steps**:
   - `actions/checkout` with `fetch-depth: 0`.
   - `actions/cache` for `.m2/repository`: To prevent exceeding GitHub's 10 GB cache quota in large enterprise repositories, cache specific internal groupIds (`.m2/repository/com/acme`) or use `key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}`.
   - `actions/cache` for `.nx/cache` (`key: ${{ runner.os }}-nx-${{ github.sha }}`).
   - `npm run test:plugin`.
   - `npx nx affected -t lint test build --parallel=3`.
   - `npx nx affected -t verify --parallel=2`.
   - `actions/upload-artifact` for test reports.
3. **Parallel-CI Transition Period**:
   - Keep the existing plain-Maven CI workflow running in parallel alongside the new `nx affected` pipeline for 2–4 weeks to validate stability before decommissioning the legacy pipeline.
4. **Dependabot**: Add `.github/dependabot.yml` for weekly `maven` and `github-actions` updates.

---

## Phase 7: Verification & Baseline Validation Protocol

After completing migration, execute this verification protocol:

1. **Plugin Unit Tests**: `node --test tools/nx-maven/` (Must pass 11/11 tests).
2. **Task Input Granularity Test (Verifies `nx.json` Caching)**:
   ```bash
   touch <any-lib>/src/test/java/.../SomeTest.java
   npx nx run-many -t build   # MUST achieve 100% cache hits
   npx nx run-many -t test    # Exactly ONE project task may re-run
   ```
3. **Affected Selectivity Test (Verifies `.nxignore`)**:
   ```bash
   touch README.md && npx nx show projects --affected
   # Must return 0 affected projects

   touch .github/workflows/ci.yml && npx nx show projects --affected
   # Must mark projects affected
   ```
4. **Nx Caching & Local `.m2` Restoration (Layout-Agnostic)**:
   ```bash
   rm -rf .m2 && find . -name target -type d -prune -exec rm -rf {} +
   npx nx run-many -t build
   find .m2/repository -name "*.jar"
   # Must restore all built module JARs to .m2 from cache
   ```
5. **Full Plain-Maven Verification**:
   ```bash
   ./mvnw -B verify
   # Must return 100% BUILD SUCCESS across all reactor modules
   ```
6. **Baseline Diff Comparison (Dependencies & Test Counts)**:
   ```bash
   ./mvnw -B dependency:tree > /tmp/after-deps.txt
   ./mvnw -B test > /tmp/after-tests.txt

   # Diff dependency tree (must be identical)
   diff /tmp/before-deps.txt /tmp/after-deps.txt

   # Diff test counts (must be identical)
   diff <(grep -h "Tests run:" /tmp/before-tests.txt) <(grep -h "Tests run:" /tmp/after-tests.txt)
   ```
7. **End-State Enforcement Ratchet**:
   Once initial dependency warnings are resolved, flip `<failOnWarning>true</failOnWarning>` on `maven-dependency-plugin` in root `pom.xml`.

---

## Phase 8: Deferred Code & Testing Conventions (POST-MIGRATION OPT-IN)

After the build migration lands cleanly, adopt the following application and testing standards incrementally:

1. **AssertJ Assertions**: Standardize 100% of unit and integration tests on AssertJ (`assertThat`).
2. **Zero Wildcard Imports**: Eliminate wildcard static imports (`import static ...*;`) across Java source and test files.
3. **Controller Unit Tests**: Use Spring `@WebMvcTest` with `MockMvc` to test parameter binding, service delegation, 200 OK, and 400 Bad Request paths.
4. **Integration Tests**: Name end-to-end integration tests `*IT.java` and execute with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and `TestRestTemplate`.
5. **Domain Error Representation**: Represent business operation errors via typed `Result<T>` (`Result.Ok<T>` / `Result.Err<T>`). Map `Result.Err` in controllers to `400 Bad Request` with `@ControllerAdvice` (`GlobalExceptionHandler`) backstops.
6. **Observability & Ports**: Add `spring-boot-starter-actuator` exposing `/actuator/health` and `/actuator/info`. Assign distinct `server.port` values in `application.yaml`.
```
