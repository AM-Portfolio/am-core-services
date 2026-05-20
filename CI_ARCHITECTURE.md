# 🏗️ am-core-services CI/CD Architecture: Monorepo with Path Filtering

This document explains the architectural decision behind keeping all `am-core-services` (Analysis, Gateway, MCP Server, Libraries, etc.) within a single repository (a **Monorepo**) and how we optimize our CI/CD pipelines using **Path Filtering**.

## 🎯 The Chosen Design
We have chosen the **Monorepo with Path Filtering** approach. 
This means all related code for `am-core-services` lives in one Git repository, but our CI/CD pipelines are intelligent enough to only build and deploy the specific microservices or libraries that actually changed.

### Why not use Multi-Repo or Git Submodules?
- **Multi-Repo (Polyrepo)** creates "versioning hell." If you update `am-common-lib`, you'd have to publish it, go to the `am-analysis` repo, update the `pom.xml`, and rebuild.
- **Git Submodules** are confusing to maintain, often cause detached HEAD errors, and don't play well with most CI/CD runners natively.

---

## 🚀 The Benefits of This Approach

1. **Atomic Commits (High Developer Velocity)**
   You can change a shared library, the domain model, and the service implementation all in one single Pull Request. You don't have to jump across three different repositories to implement one feature crossing the stack.

2. **Easy Refactoring**
   Developers can open `am-core-services` in their IDE and globally search, replace, and refactor code across all microservices and libraries instantly.

3. **Zero Unnecessary Builds**
   Because we use Path Filtering in our CI/CD tool, we completely eliminate the downside of a Monorepo. Modifying an analysis folder file will **never** trigger a gateway container build.

---

## ⚙️ How Path Filtering Works
Path filtering is a native feature in GitHub Actions. When a commit is pushed, the CI engine looks at the files modified in that commit. If the modified files match the paths declared in a workflow, that workflow runs. Otherwise, it is skipped.

### 📝 Example Workflow: Changing a single Library
1. A developer modifies `am-core-services/libraries/am-common-lib/src/main/java/.../Logger.java`.
2. They push the commit.
3. The **App Workflow** (`am-app-publish.yml`) checks: *"Did anything in the services/ folder change?"* -> **No.** (No app deployments are triggered).
4. The **SDK Workflow** (`am-sdk-publish.yml`) checks: *"Did anything in the libraries/ folder change?"* -> **Yes.** (It triggers the specific publication for `am-common-lib`).

---

## 🛠️ Pipeline Strategy: One File vs. Multiple Files?
**Question:** Should we have one giant workflow file that builds everything, or separate pipeline files for each module?

**Answer: Use Orchestrator Files at the Root.**

For `am-core-services`, we follow the standard of having high-level orchestrators:
- **`am-app-publish.yml`**: Orchestrates Docker builds and deployments for executable services.
- **`am-sdk-publish.yml`**: Orchestrates Maven publications for shared libraries and domains.

### Why this is Better:
1. **Cleaner Logic**: Instead of 20+ separate workflow files cluttering the repo, we have two clean orchestrators that delegate logic to the central `am-pipelines`.
2. **Standardized Triggering**: All services follow the same security and build rules defined in the central repository.

---

## 📦 What about SDKs and Libraries?
**SDKs and shared libraries do NOT need Docker containers.**

They are not standalone applications that run on a server. They are code dependencies that other applications import. 

### How CI deals with SDKs/Libraries:
Instead of a `docker build` step, the CI workflow for an SDK will run a **Publish** step. 
For example, if you change a class in `am-kafka-lib`:
1. The `am-sdk-publish.yml` pipeline triggers.
2. It compiles the Java code into a `.jar` file.
3. It **publishes** that `.jar` to your GitHub Package Registry. 
4. Other applications (like your `am-gateway`) can now download the new version during their own container builds.
