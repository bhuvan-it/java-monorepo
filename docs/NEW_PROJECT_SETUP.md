# Setting Up a New Nx + Maven Monorepo

This guide explains how to replicate this architecture in a **brand new repository** or migrate an existing Maven multi-module project to use Nx.

---

## 1. Core Files to Copy

To bootstrap a new workspace, copy the following infrastructure files from this repository into your new repository root:

```
new-repo/
├── .mvn/
│   ├── maven.config       <-- Configures local repo relocation (-Dmaven.repo.local=.m2/repository)
│   └── jvm.config         <-- Provides JDK compiler exports required for Spotless/formatters
├── tools/
│   └── nx-maven/
│       └── index.js       <-- Custom Nx inference plugin (scans POMs & infers targets/edges)
├── .github/
│   └── workflows/
│       └── ci.yml         <-- GitHub Actions workflow with nx affected
├── .gitignore             <-- Ignores .m2/, .nx/, target/, node_modules/
├── nx.json                <-- Nx workspace configuration
└── package.json           <-- NPM configuration declaring nx devDependency
```

---

## 2. Step-by-Step Setup Guide

### Step 1: Initialize Directory & Copy Infrastructure
Create your project directory and copy the 6 core infrastructure files listed above:

```bash
mkdir my-company-monorepo
cd my-company-monorepo

# Copy .mvn, tools, nx.json, package.json, .gitignore, .github from template
```

---

### Step 2: Configure `package.json` and Install Nx
Run `npm install` to install Nx and set up the `node_modules` bin links:

```bash
npm install
```

Verify Nx CLI is working:
```bash
npx nx --version
```

---

### Step 3: Configure Root `pom.xml`
Create the root parent/aggregator `pom.xml` in your new repository root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- Replace with your organization groupId and artifactId -->
  <groupId>com.mycompany</groupId>
  <artifactId>monorepo-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <!-- Add your child module paths here -->
    <module>libs/core-lib</module>
    <module>apps/my-app</module>
  </modules>

  <properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- Plugin versions -->
    <spotless.version>2.44.0</spotless.version>
    <jacoco.version>0.8.12</jacoco.version>
    <surefire.version>3.3.1</surefire.version>
    <failsafe.version>3.3.1</failsafe.version>
    <enforcer.version>3.5.0</enforcer.version>
    <compiler.plugin.version>3.13.0</compiler.plugin.version>
    <jacoco.line.minimum>0.00</jacoco.line.minimum>
  </properties>

  <!-- Manage third-party BOMs and internal module versions -->
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.mycompany</groupId>
        <artifactId>core-lib</artifactId>
        <version>${project.version}</version>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

---

### Step 4: Update GroupId in `nx.json`
If your company uses a different `groupId` (e.g. `com.mycompany` instead of `com.acme`), simply update the `options` object in `nx.json`:

```json
{
  "plugins": [
    {
      "plugin": "./tools/nx-maven/index.js",
      "options": {
        "groupId": "com.mycompany"
      }
    }
  ]
}
```

The plugin dynamically derives local repository paths (e.g., `{workspaceRoot}/.m2/repository/com/mycompany/${artifactId}`) and resolves dependencies matching `com.mycompany` or `${project.groupId}` automatically.

---

### Step 5: Create Child Modules
Create child modules under `libs/` or `apps/` with a `pom.xml` inheriting from `monorepo-parent`:

```xml
<parent>
  <groupId>com.mycompany</groupId>
  <artifactId>monorepo-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <relativePath>../../pom.xml</relativePath>
</parent>

<artifactId>core-lib</artifactId>
<packaging>jar</packaging>
```

---

### Step 6: Verify Workspace Setup

Run the following commands to confirm your new monorepo is operational:

1. **List project nodes**:
   ```bash
   npx nx show projects
   ```

2. **Verify dependency graph**:
   ```bash
   npx nx graph
   ```

3. **Run builds**:
   ```bash
   npx nx run-many -t build
   ```

4. **Verify raw Maven works with Nx bypassed**:
   ```bash
   mvn verify
   ```

---

## Troubleshooting Checklist

- **Issue**: `Could not find artifact com.mycompany:...` during cached build.
  - **Fix**: Verify `.mvn/maven.config` contains `-Dmaven.repo.local=.m2/repository` on its own line, and that `tools/nx-maven/index.js` outputs include `{workspaceRoot}/.m2/repository/com/mycompany/${artifactId}`.
- **Issue**: Spotless fails on modern JDKs.
  - **Fix**: Ensure `.mvn/jvm.config` includes the `--add-exports jdk.compiler/com.sun.tools.javac...` lines.
