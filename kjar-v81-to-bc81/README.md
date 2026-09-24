# Scenario 1 — Manual KJAR (JVM 17 / BAMOE 8.1) → Business Central 8.1

Demonstrates building a KJAR manually under **JDK 17 / BAMOE 8.1** and
getting it into **Business Central 8.1** via two methods:

- **Method A** — import a Git repository URL into BC (BC clones the repo and builds from source)
- **Method B** — upload the pre-built JAR directly through the BC Artifacts UI *(to be verified)*

No KIE Server interaction — this scenario is purely about getting the KJAR
into Business Central.

---

## What is in this folder

```
kjar/                        Maven project — the KJAR (packaging=kjar)
  src/main/resources/
    com/example/
      CanDrive.dmn            DMN model  — "Can Drive?" decision
      AgeRule.drl             DRL rules  — Adult / Minor classification
      HelloProcess.bpmn2      BPMN2 process — single script task
      AgeScorecard.pmml       PMML Scorecard — age → score
    META-INF/kmodule.xml      KIE module descriptor
  src/main/java/com/example/
    Applicant.java            Fact class used by DRL rules
```

---

## Prerequisites

| Requirement          | Version    |
|----------------------|------------|
| JDK                  | 17         |
| Maven                | 3.8+       |
| Business Central 8.1 | running on EAP 8.1 |

---

## Step 1 — Build the KJAR locally

```bash
cd kjar
mvn clean install
```

Expected: `BUILD SUCCESS`

The `kie-maven-plugin` validates all four model files, compiles `Applicant.java`,
and packages everything into `target/example-kjar-1.0.0.jar`.

Confirm the KJAR contents (still inside `kjar/`):

```bash
jar tf target/example-kjar-1.0.0.jar | grep -E "kmodule|\.dmn|\.drl|\.bpmn|\.pmml|kbase"
```

Expected output:

```
META-INF/kmodule.xml
META-INF/kmodule.info
META-INF/defaultKieBase/kbase.cache
com/example/CanDrive.dmn
com/example/AgeRule.drl
com/example/HelloProcess.bpmn2
com/example/AgeScorecard.pmml
```

---

## Method A — Import via Git URL in Business Central

Business Central can import any Git repository directly from its UI. You point
BC at a Git URL and it clones the repo, reads the project structure, and makes
it available for building and deploying.

### A1 — Host the KJAR sources in a Git repository

Push the `kjar/` folder to any Git hosting service (GitHub, GitLab, Gitea, or a
local bare repo) that is reachable from the machine running Business Central.

The repository must contain at its root:

```
pom.xml
src/main/resources/META-INF/kmodule.xml
src/main/resources/com/example/CanDrive.dmn
src/main/resources/com/example/AgeRule.drl
src/main/resources/com/example/HelloProcess.bpmn2
src/main/resources/com/example/AgeScorecard.pmml
src/main/java/com/example/Applicant.java
```

### A2 — Import the repository into Business Central

1. Log in to Business Central at `http://localhost:8080/business-central`
2. Click **Design** in the top menu
3. Select or create a **Space** (e.g. `MySpace`)
4. Click **Import Project**
5. Paste the Git repository URL into the **Repository URL** field
6. If the repo requires authentication, enter the credentials
7. Click **Import** — BC clones the repository and lists the discovered projects
8. Select `example-kjar` and click **OK**

### A3 — Build and deploy in BC

1. Open the imported project **example-kjar**
2. Click **Build → Build & Deploy** from the top menu
3. Wait for the build to complete — the status bar shows `Build Successful`

The KJAR is now in BC's internal Maven repository and available for deployment
to a connected KIE Server from **Deploy → Execution Servers**.

---

## Method B — Upload the pre-built JAR via the Business Central Artifacts UI *(to be verified)*

This method puts the compiled JAR directly into BC's internal artifact repository
through the BC web interface, without any Git setup.

### B1 — Open the Artifact Repository page

1. Log in to Business Central at `http://localhost:8080/business-central`
2. Click the **gear icon (⚙)** in the top-right corner
3. Select **Artifacts**

You will see BC's internal Maven repository browser.

### B2 — Upload the JAR

1. Click **Upload** (top-right of the Artifacts page)
2. Click **Choose File** and select the built JAR:

```
kjar-v81-to-bc81/kjar/target/example-kjar-1.0.0.jar
```

3. Click **Upload**

BC reads the `pom.xml` embedded inside the JAR to determine the GAV
(`com.example:example-kjar:1.0.0`) and registers it in the repository.

### B3 — Verify the artifact is listed

Search for `example-kjar` in the Artifacts page filter. You should see
`com.example:example-kjar:1.0.0` listed.

---

## Notes

### Why `default="true"` on `<kbase>` in kmodule.xml

The KIE Server DMN REST endpoint requires the container's default KieBase to
be explicitly flagged. Without `default="true"` on the `<kbase>` element,
DMN calls return `Cannot find a default KieBase`.

### Difference between Method A and Method B

|                        | Method A (Git import)                 | Method B (UI upload)                 |
|------------------------|---------------------------------------|--------------------------------------|
| What BC stores         | Source files + Git history            | Compiled JAR only                    |
| BC triggers a build?   | Yes — on import and on demand         | No                                   |
| Editable in BC UI?     | Yes — full authoring                  | No — binary only                     |
| Requires Git hosting   | Yes                                   | No                                   |
| Use when               | You want BC to own and rebuild source | You just want the artifact available |
