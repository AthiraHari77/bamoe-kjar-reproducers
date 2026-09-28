# Migration B — Manual KJAR (JDK 11 / BAMOE 8.0) → Business Central 8.1

Build a KJAR manually under **JDK 11 / BAMOE 8.0** (KIE 7.67.x) and import it
into **Business Central 8.1** via Git or direct upload.

---

## What is in this folder

```
kjar/                          BAMOE 8.0 KJAR (packaging=kjar)
  src/main/resources/com/example/
    CanDrive.dmn                DMN — "Can Drive?" decision
    AgeRule.drl                 DRL — Adult / Minor classification
    HelloProcess.bpmn2          BPMN2 — single script task
    AgeScorecard.pmml           PMML Scorecard — age → score
  META-INF/kmodule.xml          KIE module descriptor
  src/main/java/com/example/
    Applicant.java              Fact class used by DRL rules
```

---

## Prerequisites

| Requirement          | Version |
|----------------------|---------|
| JDK                  | 11      |
| Maven                | 3.6+    |
| Business Central 8.1 | running |

---

## Step 1 — Build the KJAR (with JDK 11)

```bash
cd kjar
mvn clean install
```

Expected: `BUILD SUCCESS`

---

## Method A — Import via Git

### A1 — Push the KJAR source to a Git repository

Push the `kjar/` folder contents to any Git hosting service reachable from BC 8.1.
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

### A2 — Import into Business Central 8.1

1. Log in to Business Central 8.1 at `http://localhost:8080/business-central`
2. Go to **Design** → select or create a Space
3. Click **Import Project**
4. Paste the Git repository URL and click **Import**
5. Select `manual-v80-kjar` and click **OK**

### A3 — Build and deploy in BC 8.1

1. Open the project **manual-v80-kjar**
2. Click **Build → Build & Deploy**
3. Wait for `Build Successful`

The KJAR is now in BC's internal Maven repository and available for deployment from **Deploy → Execution Servers**.

---

## Method B — Upload via Artifacts UI 

### B1 — Open Artifacts

1. Log in to Business Central 8.1 at `http://localhost:8080/business-central`
2. Click the **gear icon (⚙)** → **Artifacts**

### B2 — Upload the JAR

1. Click **Upload**
2. Select `kjar/target/manual-v80-kjar-1.0.0.jar`
3. Click **Upload**

BC reads the embedded `pom.xml` and registers `com.example:manual-v80-kjar:1.0.0`.

---

## Notes

| | Method A (Git) | Method B (Upload) |
|---|---|---|
| BC stores | Source + Git history | Compiled JAR only |
| BC builds? | Yes | No |
| Editable in BC? | Yes | No |
| Use when | You want BC to own and rebuild source | You just need the artifact available |
