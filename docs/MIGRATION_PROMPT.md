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

## Phase 0: Discovery & Baseline Capture (PRE-MIGRATION SAFEGUARDS)

Before modifying any source code or configuration files, execute discovery and record baselines:

1. **Inventory Current Workspace**:
   - Record JDK version, Maven version, packaging types, module layout, and existing CI workflows.
   - Identify existing `.mvn/` configurations, `settings.xml` mirrors, and custom release/deployment plugins.
2. **Coordinate Stability Guarantee**:
   - `groupId`, `artifactId`, and `version` coordinates of existing modules MUST remain 100% stable so downstream consumers are not broken.
3. **Capture Baseline Artifacts**:
   Run the following baseline commands and save outputs:
   ```bash
   ./mvnw -B dependency:tree > /tmp/before-deps.txt
   ./mvnw -B test > /tmp/before-tests.txt
   ```
   After migration, verify that the dependency tree and test counts match the baseline exactly.

---

## Phase 1: Local Maven Relocation & Wrapper Setup

1. **Maven Wrapper**: Ensure Maven Wrapper (`./mvnw` and `mvnw.cmd`) is present using Maven 3.9.9 (`only-script` distribution). All commands MUST shell `./mvnw` (or `mvnw.cmd`).
2. **Local Repository Relocation**: Create `.mvn/maven.config` containing:
   ```
   -Dmaven.repo.local=.m2/repository
   ```
   This isolates Maven dependencies inside `.m2/repository` in the workspace root, enabling Nx to cache built module JARs.
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
- **Graph Dependencies (`createDependencies`)**: Iterate over `context.projects` to construct graph edges matching inter-module POM `<dependencies>` and `<parent>` references. Support groupId prefix matching or groupId arrays to handle multi-groupId enterprise repositories.

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

### 1. `nx.json` (Explicit Task Granularity & Dependency Order)
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
  "plugins": ["./tools/nx-maven/index.js"]
}
```

### 2. `package.json`
Pin `nx` exactly and define workspace scripts:
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
    "nx": "19.5.6"
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
   - Bind `analyze-only` to `verify` phase. In migration mode, set `<failOnWarning>false</failOnWarning>` initially to log undeclared/unused dependencies without breaking existing builds.
3. **Spotless Code Formatter (`spotless-maven-plugin`)**:
   - Configure `palantirJavaFormat`, `removeUnusedImports`, `importOrder`, `trimTrailingWhitespace`, `endWithNewline`.
   - Use `<ratchetFrom>origin/main</ratchetFrom>` so formatting rules only apply to modified files and do not disrupt `git blame` across untouched legacy code.
4. **JaCoCo Coverage Ratcheting (`jacoco-maven-plugin`)**:
   - Bind `prepare-agent` and `report` to `test` phase, and `check` to `verify` phase.
   - Set `<jacoco.line.minimum>` to the **measured baseline coverage** of the codebase, ratcheting up over time.
5. **Surefire & Failsafe Plugins**:
   - `maven-surefire-plugin`: Include `**/*Test.java`, exclude `**/*IT.java`, set `<argLine>@{argLine}</argLine>`.
   - `maven-failsafe-plugin`: Include `**/*IT.java`, bind `integration-test` and `verify` goals.
6. **CycloneDX SBOM (`cyclonedx-maven-plugin`)**:
   - Bind `makeAggregateBom` to `package` phase, producing `bom.json` and `bom.xml`.

---

## Phase 5: Spring Boot & Aggregate Coverage

1. **Spring Boot Exec Classifier**: Configure `spring-boot-maven-plugin` with `<classifier>exec</classifier>` in application POMs so standard library JARs remain available for compile-time/JaCoCo dependencies alongside executable archives.
2. **Aggregate Coverage Module**: Create `libs/coverage-aggregate` (`<packaging>pom</packaging>`) depending on all code modules and running `jacoco:report-aggregate` on `verify`.

---

## Phase 6: Supply-Chain & CI Workflow (`.github/workflows/ci.yml`)

1. **Action SHA Pinning**: Pin all GitHub Actions to 40-character commit SHAs.
2. **CI Pipeline Steps**:
   - `fetch-depth: 0` on checkout.
   - `npm run test:plugin`.
   - `npx nx affected -t lint test build`.
   - `npx nx affected -t verify`.
   - `./mvnw -B verify` (Plain-Maven escape hatch job).
3. **Dependabot**: Add `.github/dependabot.yml` for weekly `maven` and `github-actions` updates.

---

## Phase 7: Verification & Validation Protocol

After completing migration, execute this verification protocol:

1. **Plugin Unit Tests**: `node --test tools/nx-maven/` (Must pass all tests).
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
4. **Nx Caching & Local `.m2` Restoration**:
   ```bash
   rm -rf .m2 libs/*/target apps/*/target
   npx nx run-many -t build
   find .m2/repository -name "*.jar"
   # Must restore all module JARs to .m2 from cache
   ```
5. **Baseline Comparison**:
   ```bash
   ./mvnw -B dependency:tree > /tmp/after-deps.txt
   diff /tmp/before-deps.txt /tmp/after-deps.txt
   # Must match baseline dependency tree
   ```
```
