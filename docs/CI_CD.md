# CI/CD Pipeline Guide

This repository includes a pre-configured GitHub Actions CI workflow in `.github/workflows/ci.yml`.

---

## CI Pipeline Overview

The CI pipeline runs on every `push` to `main` and on all `pull_request` events targeting `main`.

### Pipeline Steps:

1. **Checkout**: Checks out repository code with `fetch-depth: 0` (required for `nx affected` to calculate commit history).
2. **Derive Base SHA**: Uses `nrwl/nx-set-shas@v4` to compute the `base` and `head` SHAs for affected task comparison.
3. **Setup Java & Node**:
   - `actions/setup-java@v4` (Temurin JDK 21)
   - `actions/setup-node@v4` (Node 22)
4. **Cache Dependencies & Build Artifacts**:
   - `actions/cache@v4` caches `.m2/repository` keyed on `pom.xml` hashes.
   - `actions/cache@v4` caches `.nx/cache` keyed on commit SHA.
5. **Install Node Tools**: Executes `npm ci`.
6. **Execute Affected Tasks**:
   ```bash
   npx nx affected -t lint test build --parallel=3
   ```

---

## Running CI Workflows Locally

You can test CI behavior on your local branch using Nx affected:

```bash
# Compare local changes against main
npx nx affected -t lint test build
```

To run tasks across all projects regardless of changes:

```bash
npx nx run-many -t lint test build --parallel=3
```
