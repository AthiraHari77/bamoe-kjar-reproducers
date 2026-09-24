# Migration A — BC 8.0 KJAR → Business Central 8.1

Demonstrates taking a KJAR that was **authored in / exported from Business Central 8.0**
(JDK 11 / KIE 7.67.2.Final-redhat-00034) and importing it into **Business Central 8.1**.

The KJAR contains all four model types plus Test Scenario (scesim) files that can be
run inside BC to verify the models after import.

---

## What is in this folder

```
kjar/                          Maven project — BAMOE 8.0 KJAR (packaging=kjar)
  src/main/resources/
    com/example/
      CanDrive.dmn              DMN model  — "Can Drive?" decision
      AgeRule.drl               DRL rules  — Adult / Minor classification
      HelloProcess.bpmn2        BPMN2 process — single script task
      AgeScorecard.pmml         PMML Scorecard — age → score
    META-INF/kmodule.xml        KIE module descriptor
  src/main/java/com/example/
    Applicant.java              Fact class used by DRL rules
  src/test/resources/com/example/
    CanDrive.scesim             Test Scenario for DMN — verifies Can Drive? decision
    AgeRule.scesim              Test Scenario for DRL — verifies Adult/Minor rule firing
  src/test/java/testscenario/
    ScenarioJunitActivatorTest.java   JUnit activator for scesim tests
```

---

## Prerequisites

| Requirement          | Version                        |
|----------------------|--------------------------------|
| JDK                  | 11 (to build the 8.0 KJAR)     |
| Maven                | 3.6+                           |
| Business Central 8.1 | running on EAP 8.1             |

---

## Step 1 — Build the KJAR locally (optional verification)

This step confirms the KJAR builds and all scesim tests pass under JDK 11
before importing into BC 8.1.

```bash
cd kjar
mvn clean install
```

Expected: `BUILD SUCCESS` with scesim tests passing:

```
Tests run: 2, Failures: 0, Errors: 0  (CanDrive.scesim)
Tests run: 2, Failures: 0, Errors: 0  (AgeRule.scesim)
```

---

## Method A — Import via Git URL in Business Central 8.1

### A1 — Host the KJAR sources in a Git repository

Push the `kjar/` folder contents to any Git hosting service reachable from
Business Central 8.1 (GitHub, GitLab, Gitea, or a local bare repo).

The repository root must contain:

```
pom.xml
src/main/resources/META-INF/kmodule.xml
src/main/resources/com/example/CanDrive.dmn
src/main/resources/com/example/AgeRule.drl
src/main/resources/com/example/HelloProcess.bpmn2
src/main/resources/com/example/AgeScorecard.pmml
src/main/java/com/example/Applicant.java
src/test/resources/com/example/CanDrive.scesim
src/test/resources/com/example/AgeRule.scesim
src/test/java/testscenario/ScenarioJunitActivatorTest.java
```

### A2 — Import the repository into Business Central 8.1

1. Log in to Business Central 8.1 at `http://localhost:8080/business-central`
2. Click **Design** in the top menu
3. Select or create a **Space** (e.g. `MySpace`)
4. Click **Import Project**
5. Paste the Git repository URL into the **Repository URL** field
6. Enter credentials if required
7. Click **Import** — BC clones the repo and lists discovered projects
8. Select `example-kjar` and click **OK**

### A3 — Build and verify in BC 8.1

1. Open the imported project **example-kjar**
2. Click **Build → Build & Deploy**
3. Wait for `Build Successful`

The scesim tests run automatically as part of the build. To run them explicitly:

1. Open `CanDrive.scesim` or `AgeRule.scesim` from the project file tree
2. Click the **Run** (▶) button in the Test Scenario editor
3. Confirm all scenarios pass (green)

---

## Method B — Upload the pre-built JAR via the Business Central Artifacts UI *(to be verified)*

### B1 — Open the Artifact Repository page

1. Log in to Business Central at `http://localhost:8080/business-central`
2. Click the **gear icon (⚙)** in the top-right corner
3. Select **Artifacts**

### B2 — Upload the JAR

1. Click **Upload**
2. Click **Choose File** and select:

```
kjar-bc80-to-bc81/kjar/target/example-kjar-1.0.0.jar
```

3. Click **Upload**

BC reads the embedded `pom.xml` to determine the GAV
(`com.example:example-kjar:1.0.0`) and registers it in the repository.

> **Note:** The uploaded JAR is a binary — Test Scenario files inside it are
> not runnable from the BC UI. Use Method A (Git import) if you need to run
> or edit the scesim files in BC.

### B3 — Verify the artifact is listed

Search for `example-kjar` in the Artifacts page filter. You should see
`com.example:example-kjar:1.0.0` listed.

---

## Notes

### scesim test coverage

| File              | Model type | Scenarios                                 |
|-------------------|------------|-------------------------------------------|
| `CanDrive.scesim` | DMN        | Age=25 → `Can Drive?=true`; Age=15 → `false` |
| `AgeRule.scesim`  | DRL        | age=25 → `[ADULT:25]`; age=15 → `[MINOR:15]` |

### Difference between Method A and Method B

|                        | Method A (Git import)                 | Method B (UI upload)                 |
|------------------------|---------------------------------------|--------------------------------------|
| What BC stores         | Source files + Git history            | Compiled JAR only                    |
| BC triggers a build?   | Yes — on import and on demand         | No                                   |
| scesim runnable in BC? | Yes                                   | No                                   |
| Editable in BC UI?     | Yes — full authoring                  | No — binary only                     |
| Use when               | You want BC to own and rebuild source | You just want the artifact available |
