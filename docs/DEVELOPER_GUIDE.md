# Developer Guide: Acme Java Monorepo

Welcome to the Acme Java Monorepo! This repository uses **Nx** for computation caching and task orchestration, and **Maven** for the Java 21 build lifecycle.

---

## Prerequisites

Before starting, ensure you have the following installed on your machine:

1.- **Java 21 JDK**: Installed and configured on `PATH`.
- **Node.js 18+ & npm**: Required to run Nx task orchestrator.
- **Maven Wrapper (`./mvnw`)**: Included in the repository (Maven 3.9.9); no standalone Maven installation required.

Verify installations:
```bash
java -version
node -v
mvn -version
```

---

## Quick Start

1. **Clone the repository**:
   ```bash
   git clone https://github.com/bhuvan-it/java-monorepo.git
   cd java-monorepo
   ```

2. **Install Node dependencies**:
   ```bash
   npm install
   ```

3. **Show workspace projects**:
   ```bash
   npx nx show projects
   ```

4. **Build an application (e.g., `order-service`)**:
   ```bash
   npx nx build order-service
   ```
   *Nx will automatically build all upstream dependencies (`common-core`, `domain-model`, `persistence`, `common-logging`, and `monorepo-parent`) in topological order first!*

---

## Common Developer Commands

### Building Code

- **Build a single application or library**:
  ```bash
  npx nx build order-service
  npx nx build common-core
  ```

- **Build all projects in the workspace**:
  ```bash
  npx nx run-many -t build
  ```

---

### Running Tests

- **Run unit tests for a specific project**:
  ```bash
  npx nx test order-service
  ```

- **Run integration tests (Failsafe `*IT.java`) for a project**:
  ```bash
  npx nx verify order-service
  ```

- **Run tests across all projects**:
  ```bash
  npx nx run-many -t test
  ```

---

### Code Style & Formatting (Spotless)

- **Check code formatting across the repository**:
  ```bash
  npx nx run-many -t lint
  ```

- **Automatically apply code formatting to all files**:
  ```bash
  npx nx run-many -t format
  ```
  *Or via npm script*: `npm run format`

---

### Running Applications

- **Run Spring Boot applications locally**:
  ```bash
  # Order Service runs on http://localhost:8081
  npx nx serve order-service

  # Inventory Service runs on http://localhost:8082
  npx nx serve inventory-service
  ```
  *Both services can be run concurrently in separate terminals without port conflicts.*

---

### Visualizing the Workspace

- **Open interactive graph in browser**:
  ```bash
  npx nx graph
  ```

- **Export graph to JSON**:
  ```bash
  npx nx graph --file=graph.json
  ```

---

### Running Affected Tasks (CI / Local Feature Branches)

Run tasks only on modules modified relative to `main` branch:

```bash
npx nx affected -t lint test build
```

---

## IDE Setup

### IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA and select **File -> Open**.
2. Select the repository root folder (`java-monorepo`).
3. IntelliJ will automatically detect the root `pom.xml` and import all 7 modules.
4. **Nx Console Plugin (Optional)**: Install the official **Nx Console** plugin from IntelliJ Plugin Marketplace to run Nx tasks directly from an IDE tool window.

### VS Code

1. Open the repository root folder in VS Code.
2. Install recommended extensions:
   - **Nx Console** (`nrwl.angular-console`)
   - **Extension Pack for Java** (`vscjava.vscode-java-pack`)
