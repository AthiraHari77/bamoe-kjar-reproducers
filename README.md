# BAMOE 8.1 — KJAR Scenario Reproducers

This repository contains seven self-contained scenarios covering how to get a KJAR and where to deploy it, across native BAMOE 8.1 and migration (8.0 → 8.1) paths.

| Folder | Scenario |
|---|---|
| [`kjar-bc81-to-ks81`](kjar-bc81-to-ks81/) | **A** — BC 8.1 → KIE Server 8.1 |
| [`kjar-v81-to-bc81`](kjar-v81-to-bc81/) | **B** — Manual KJAR (JDK 17) → BC 8.1 |
| [`kjar-v81-to-ks81`](kjar-v81-to-ks81/) | **C** — Manual KJAR (JDK 17) → KIE Server 8.1 |
| [`kjar-bc80-to-bc81`](kjar-bc80-to-bc81/) | **Migration A** — BC 8.0 → BC 8.1 |
| [`kjar-v80-to-bc81`](kjar-v80-to-bc81/) | **Migration B** — Manual KJAR (JDK 11) → BC 8.1 |
| [`kjar-bc80-to-ks81`](kjar-bc80-to-ks81/) | **Migration C** — BC 8.0 → KIE Server 8.1 |
| [`kjar-v80-to-ks81`](kjar-v80-to-ks81/) | **Migration D** — Manual KJAR (JDK 11) → KIE Server 8.1 |

---

## Prerequisites

|       | Native 8.1 scenarios | Migration (8.0 → 8.1) |
|-------|----------------------|-----------------------|
| JDK   | 17                   | 11                    |
| Maven | 3.8+                 | 3.8+                  |

KIE Server must be running in **managed mode** for BC-based deployments — started with `org.kie.server.controller` pointing at Business Central.

---

## Scenario A — BC 8.1 → KIE Server 8.1

### Author assets in BC 8.1

Create a project in BC 8.1 with the following assets in package `com.example`: a DMN decision, a Data Object (fact class), a DRL rule, a BPMN2 process, and a PMML model.

### Verify with Scenario Simulations

Add Test Scenario assets for the DMN and DRL models. Run each with **▶ Run** — all rows must pass before proceeding.

### Deploy to KIE Server 8.1

In BC 8.1 go to **Deploy → Execution Servers**, select the KIE Server, click **Add Container**, fill in the project coordinates, and click **Finish → Deploy**. The container status should change to **Started**.

### Execute

Use the KIE Server REST API or the Java client to call DMN, DRL, BPMN, and PMML endpoints on the deployed container. For example, to evaluate a DMN decision:

```bash
curl -s -u <user>:<password> -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "http://<host>:<port>/kie-server/services/rest/server/containers/<container-id>/dmn" \
  -d '{"model-namespace":"http://www.example.com/CanDrive","model-name":"CanDrive","dmn-context":{"Age":25}}'
```

Expected response: `"Can Drive?": true`

Repeat the same for DRL, BPMN, and PMML using their respective endpoints and payloads.

---

## Scenario B — Manual KJAR (JDK 17) → BC 8.1

### Build the KJAR

Build the KJAR locally using JDK 17 with `mvn clean install`.

### Import into BC 8.1

**Via Git:** Push the project source to a Git repository, then in BC 8.1 use **Design → Import Project**, paste the Git URL, and click **Build → Build & Deploy**.

**Via Artifacts upload:** Upload the compiled JAR via **⚙ → Artifacts → Upload**, then deploy a container from **Deploy → Execution Servers → Add Container**.

---

## Scenario C — Manual KJAR (JDK 17) → KIE Server 8.1

### Build and install the KJAR

Build the KJAR locally using JDK 17. Use `mvn deploy` with `altDeploymentRepository` pointing at the KIE Server's embedded Maven repository to install the JAR directly into the right location.

### Deploy and execute

Deploy the container via the KIE Server REST API:

```bash
curl -s -u <user>:<password> -X PUT \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "http://<host>:<port>/kie-server/services/rest/server/containers/<container-id>" \
  -d '{"container-id":"<container-id>","release-id":{"group-id":"<groupId>","artifact-id":"<artifactId>","version":"<version>"}}'
```

Then execute DMN, DRL, BPMN, and PMML endpoints using curl or the Java client.

---

## Migration A — BC 8.0 → BC 8.1

### Get the KJAR from BC 8.0

In BC 8.0, open the project and click **Build → Build & Download** to download the compiled JAR.

### Import into BC 8.1

**Via Git:** Push the downloaded project source to a Git repository, then in BC 8.1 use **Design → Import Project**, paste the Git URL, and click **Build → Build & Deploy**. Scesim tests can be run from the BC 8.1 UI after import.

**Via Artifacts upload:** Upload the JAR via **⚙ → Artifacts → Upload**, then deploy a container from **Deploy → Execution Servers → Add Container**.

> Scesim tests are only runnable via the Git import method — binary uploads do not support running scesim from the BC UI.

---

## Migration B — Manual KJAR (JDK 11) → BC 8.1

### Build the KJAR

Build the KJAR locally using JDK 11 with `mvn clean install`.

### Import into BC 8.1

**Via Git:** Push the project source to a Git repository, then in BC 8.1 use **Design → Import Project**, paste the Git URL, and click **Build → Build & Deploy**.

**Via Artifacts upload:** Upload the compiled JAR via **⚙ → Artifacts → Upload**, then deploy a container from **Deploy → Execution Servers → Add Container**.

---

## Migration C — BC 8.0 → KIE Server 8.1

### Get the KJAR from BC 8.0

In BC 8.0, open the project and click **Build → Build & Download** to download the compiled JAR.

### Install and deploy

Use `mvn install:install-file` to install the downloaded JAR into the KIE Server's embedded Maven repository, then deploy the container via the KIE Server REST API:

```bash
curl -s -u <user>:<password> -X PUT \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "http://<host>:<port>/kie-server/services/rest/server/containers/<container-id>" \
  -d '{"container-id":"<container-id>","release-id":{"group-id":"<groupId>","artifact-id":"<artifactId>","version":"<version>"}}'
```

### Execute

Use curl or the Java client to call DMN, DRL, BPMN, and PMML endpoints on the deployed container.

---

## Migration D — Manual KJAR (JDK 11) → KIE Server 8.1

### Build and install the KJAR

Build the KJAR locally using JDK 11. Use `mvn deploy` with `altDeploymentRepository` pointing at the KIE Server's embedded Maven repository to install the JAR directly into the right location.

### Deploy and execute

Deploy the container via the KIE Server REST API:

```bash
curl -s -u <user>:<password> -X PUT \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "http://<host>:<port>/kie-server/services/rest/server/containers/<container-id>" \
  -d '{"container-id":"<container-id>","release-id":{"group-id":"<groupId>","artifact-id":"<artifactId>","version":"<version>"}}'
```

Then execute DMN, DRL, BPMN, and PMML endpoints using curl or the Java client.
