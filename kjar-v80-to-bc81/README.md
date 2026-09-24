# Migration B — Manual KJAR v8.0 (JVM 11 / BAMOE 8.0) → Business Central 8.1

Demonstrates building a KJAR **manually** under **JDK 11 / BAMOE 8.0**
(KIE 7.67.2.Final-redhat-00034) and importing it into **Business Central 8.1**.

This is the v8.0 equivalent of Scenario 1 — the KJAR is hand-crafted locally
(not authored inside Business Central) and then brought into BC 8.1.

---

## What is in this folder

```
kjar/                          Maven project — manual BAMOE 8.0 KJAR (packaging=kjar)
  src/main/resources/
    com/example/
      CanDrive.dmn              DMN model  — "Can Drive?" decision
      AgeRule.drl               DRL rules  — Adult / Minor classification
      HelloProcess.bpmn2        BPMN2 process — single script task
      AgeScorecard.pmml         PMML Scorecard — age → score
    META-INF/kmodule.xml        KIE module descriptor
  src/main/java/com/example/
    Applicant.java              Fact class used by DRL rules
```

---

## Prerequisites

| Requirement          | Version                        |
|----------------------|--------------------------------|
| JDK                  | 11                             |
| Maven                | 3.6+                           |
| Business Central 8.1 | running on EAP 8.1             |

---

## Step 1 — Build the KJAR locally

```bash
cd kjar
mvn clean install
```

Expected: `BUILD SUCCESS`

The `kie-maven-plugin` validates all four model files, compiles `Applicant.java`,
and packages everything into `target/manual-v80-kjar-1.0.0.jar`.

Confirm the KJAR contents (still inside `kjar/`):

```bash
jar tf target/manual-v80-kjar-1.0.0.jar | grep -E "kmodule|\.dmn|\.drl|\.bpmn|\.pmml|kbase"
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
```

### A2 — Import the repository into Business Central 8.1

1. Log in to Business Central 8.1 at `http://localhost:8080/business-central`
2. Click **Design** in the top menu
3. Select or create a **Space** (e.g. `MySpace`)
4. Click **Import Project**
5. Paste the Git repository URL into the **Repository URL** field
6. Enter credentials if required
7. Click **Import** — BC clones the repo and lists discovered projects
8. Select `manual-v80-kjar` and click **OK**

### A3 — Build and deploy in BC 8.1

1. Open the imported project **manual-v80-kjar**
2. Click **Build → Build & Deploy**
3. Wait for `Build Successful`

The KJAR is now in BC's internal Maven repository and available for deployment
to a connected KIE Server from **Deploy → Execution Servers**.

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
kjar-v80-to-bc81/kjar/target/manual-v80-kjar-1.0.0.jar
```

3. Click **Upload**

BC reads the embedded `pom.xml` to determine the GAV and registers the artifact.

### B3 — Verify the artifact is listed

Search for `manual-v80-kjar` in the Artifacts page filter. You should see
`com.example:manual-v80-kjar:1.0.0` listed.

---

## Notes

### Difference between Method A and Method B

|                        | Method A (Git import)                 | Method B (UI upload)                 |
|------------------------|---------------------------------------|--------------------------------------|
| What BC stores         | Source files + Git history            | Compiled JAR only                    |
| BC triggers a build?   | Yes — on import and on demand         | No                                   |
| Editable in BC UI?     | Yes — full authoring                  | No — binary only                     |
| Use when               | You want BC to own and rebuild source | You just want the artifact available |

### JDK version

This KJAR targets **JDK 11 / BAMOE 8.0**. BC 8.1 can import and build it, but
if you want to run it against a KIE Server 8.1 environment see
[`../kjar-v80-to-ks81/README.md`](../kjar-v80-to-ks81/README.md).
