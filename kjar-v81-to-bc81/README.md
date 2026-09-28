# Scenario B — Manual KJAR (JDK 17 / BAMOE 8.1) → Business Central 8.1

Build a KJAR manually under **JDK 17 / BAMOE 8.1** and import it into
**Business Central 8.1** via Git or direct upload.

---

## What is in this folder

```
kjar/                        Maven project — the KJAR (packaging=kjar)
  src/main/resources/com/example/
    CanDrive.dmn              DMN — "Can Drive?" decision
    AgeRule.drl               DRL — Adult / Minor classification
    HelloProcess.bpmn2        BPMN2 — single script task
    AgeScorecard.pmml         PMML Scorecard — age → score
  META-INF/kmodule.xml        KIE module descriptor
  src/main/java/com/example/
    Applicant.java            Fact class used by DRL rules
```

---

## Prerequisites

| Requirement          | Version |
|----------------------|---------|
| JDK                  | 17      |
| Maven                | 3.8+    |
| Business Central 8.1 | running |

---

## Step 1 — Build the KJAR

```bash
cd kjar
mvn clean install
```

Expected: `BUILD SUCCESS`

---

## Method A — Import via Git

### A1 — Push to a Git repository

Push the `kjar/` folder contents to any Git host reachable from BC (GitHub, GitLab, Gitea, or a local bare repo). The repository root must contain:

```
pom.xml
src/main/resources/META-INF/kmodule.xml
src/main/resources/com/example/CanDrive.dmn
src/main/resources/com/example/AgeRule.drl
src/main/resources/com/example/HelloProcess.bpmn2
src/main/resources/com/example/AgeScorecard.pmml
src/main/java/com/example/Applicant.java
```

### A2 — Import into BC

**Design → Import Project**, paste the Git URL, select `example-kjar`, click **OK**.

### A3 — Build and deploy

Open the project, click **Build → Build & Deploy**, wait for `Build Successful`.

The KJAR is now in BC's internal Maven repo and the container is deployed to the connected KIE Server.

---

## Method B — Upload via Artifacts UI

### B1 — Upload to Artifacts

**⚙ → Artifacts → Upload**, select `kjar/target/example-kjar-1.0.0.jar`.

BC registers `com.example:example-kjar:1.0.0` in its internal Maven repo.

### B2 — Deploy a container

**Deploy → Execution Servers → Add Container**, fill in `com.example` / `example-kjar` / `1.0.0`, click **Finish → Deploy**.

> Requires KIE Server running in managed mode (started with `org.kie.server.controller` pointing at BC). Without it, BC shows "No Remote Servers" and the Deploy button has no effect.

---

## Notes

|                 | Method A (Git)                        | Method B (Upload)                    |
|-----------------|---------------------------------------|--------------------------------------|
| BC stores       | Source + Git history                  | Compiled JAR only                    |
| BC builds?      | Yes                                   | No                                   |
| Editable in BC? | Yes                                   | No                                   |
| Use when        | You want BC to own and rebuild source | You just need the artifact available |
