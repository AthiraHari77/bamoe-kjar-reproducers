# BAMOE 8.1 — KJAR Scenario Guide

Quick reference for all seven scenarios. Each scenario covers how to get a KJAR and where to deploy it.

---

## Prerequisites

| | Native 8.1 scenarios | Migration (8.0 → 8.1) |
|---|---|---|
| JDK | 17 | 11 (to build 8.0 KJAR) |
| Maven | 3.8+ | 3.6+ |

```bash
export KS_URL=http://localhost:8080/kie-server/services/rest/server
export KS_USER=adminUser
export KS_PASS=admin@Redhat1
export EAP81=$HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1
```

---

## Scenario A — BC 8.1 → KIE Server 8.1

**Author in BC 8.1, deploy to KIE Server 8.1.**

### Author

In BC 8.1, create a project with the following assets (package `com.example`):

| Asset | Type |
|-------|------|
| `CanDrive` | DMN |
| `Applicant` | Data Object (field: `age` Integer) |
| `AgeRule` | DRL |
| `HelloProcess` | Business Process (BPMN2) |
| `AgeScorecard` | PMML |

### Verify with Scenario Simulations

Add Test Scenario assets and run each with **▶ Run** — all rows must pass:

| Simulation | Type | Test |
|------------|------|------|
| `CanDrive` | DMN | Age=25 → `Can Drive?`=true, Age=15 → false |
| `AgeRule` | Rule | Applicant.age=25 and 15 round-trip unchanged |

### Deploy to KIE Server 8.1

**Deploy → Execution Servers → Add Container**, fill in `com.example` / `example-kjar` / `1.0.0`, click **Finish → Deploy**.

> Requires KIE Server in managed mode (`org.kie.server.controller` pointing at BC). Container status should change to **Started**.

### Execute via curl

**DMN:**
```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/dmn" \
  -d '{"model-namespace":"http://www.example.com/CanDrive","model-name":"CanDrive","dmn-context":{"Age":25}}'
```
Expected: `"Can Drive?": true`

**DRL:**
```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{"lookup":"defaultStatelessKieSession","commands":[{"set-global":{"identifier":"results","object":{"java.util.ArrayList":[]},"out-identifier":"results"}},{"insert":{"object":{"com.example.Applicant":{"age":25}}}},{"fire-all-rules":{"out-identifier":"fired"}}]}'
```
Expected: `"value": ["ADULT:25"]`

**BPMN:**
```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/processes/com.example.HelloProcess/instances" \
  -d '{}'
```
Expected: a numeric process instance ID

**PMML:**
```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{"lookup":"defaultKieSession","commands":[{"apply-pmml-model-command":{"outIdentifier":"pmml-result","requestData":{"correlationId":"1","modelName":"AgeScorecard","source":"com/example/AgeScorecard.pmml","requestParams":[{"name":"age","type":"java.lang.Double","value":"25.0"}]}}}]}'
```
Expected: `"score": 10.0`

### Execute via Java client

```bash
cd scenario-reproducers/kjar-bc81-to-ks81/java-client
mvn compile exec:java -Dexec.mainClass=com.example.client.DmnExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

mvn exec:java -Dexec.mainClass=com.example.client.DrlExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

mvn exec:java -Dexec.mainClass=com.example.client.BpmnExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

mvn exec:java -Dexec.mainClass=com.example.client.PmmlExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"
```

---

## Scenario B — Manual KJAR (JDK 17) → BC 8.1

**Build KJAR manually with JDK 17, import into BC 8.1.**

```bash
cd kjar/
mvn clean install
```

**Via Git:**
```bash
git init && git add . && git commit -m "KJAR"
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin master
```
In BC 8.1: **Design → Import Project** → paste Git URL → **Build → Build & Deploy**

**Via Artifacts upload *(to be verified)*:**
In BC 8.1: **⚙ → Artifacts → Upload** → select JAR → **Deploy → Execution Servers → Add Container**

> Requires KIE Server in managed mode for the Artifacts upload path.

---

## Scenario C — Manual KJAR (JDK 17) → KIE Server 8.1

**Build KJAR manually with JDK 17, deploy directly to KIE Server 8.1.**

```bash
cd kjar/
mvn clean deploy -DskipTests \
  -DaltDeploymentRepository="kie-server::default::file:$EAP81/repositories/kie/global"

curl -s -u "$KS_USER:$KS_PASS" -X PUT \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0" \
  -d '{"container-id":"example-kjar_1.0.0","release-id":{"group-id":"com.example","artifact-id":"example-kjar","version":"1.0.0"}}'
```

---

## Migration A — BC 8.0 → BC 8.1

**Download KJAR from BC 8.0, import into BC 8.1.**

In BC 8.0: **Build → Build & Download**

**Via Git:**
```bash
cd /path/to/downloaded-project   # where pom.xml lives
git init && git add . && git commit -m "BAMOE 8.0 KJAR"
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin master
```
In BC 8.1: **Design → Import Project** → paste Git URL → **Build → Build & Deploy**

**Via Artifacts upload *(to be verified)*:**
In BC 8.1: **⚙ → Artifacts → Upload** → select JAR → **Deploy → Execution Servers → Add Container**

> Scesim tests only work with the Git method (source import). Binary upload does not support running scesim from BC UI.

---

## Migration B — Manual KJAR (JDK 11) → BC 8.1

**Build KJAR manually with JDK 11, import into BC 8.1.**

```bash
cd kjar/
mvn clean install
```

**Via Git:**
```bash
git init && git add . && git commit -m "BAMOE 8.0 KJAR"
git remote add origin https://github.com/<user>/<repo>.git
git push -u origin master
```
In BC 8.1: **Design → Import Project** → paste Git URL → **Build → Build & Deploy**

**Via Artifacts upload *(to be verified)*:**
In BC 8.1: **⚙ → Artifacts → Upload** → select JAR → **Deploy → Execution Servers → Add Container**

---

## Migration C — BC 8.0 → KIE Server 8.1

**Download KJAR from BC 8.0, deploy directly to KIE Server 8.1.**

In BC 8.0: **Build → Build & Download**

```bash
export JAR_PATH="/path/to/downloaded.jar"

mvn install:install-file \
  -Dfile="$JAR_PATH" \
  -DgroupId=com.example -DartifactId=example-kjar -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DlocalRepositoryPath="$EAP81/repositories/kie/global"

curl -s -u "$KS_USER:$KS_PASS" -X PUT \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0" \
  -d '{"container-id":"example-kjar_1.0.0","release-id":{"group-id":"com.example","artifact-id":"example-kjar","version":"1.0.0"}}'
```

> KIE Server 8.1 is fully backward-compatible with BAMOE 8.0 KJARs — no changes required.

---

## Migration D — Manual KJAR (JDK 11) → KIE Server 8.1

**Build KJAR manually with JDK 11, deploy directly to KIE Server 8.1.**

```bash
cd kjar/
mvn clean deploy -DskipTests \
  -DaltDeploymentRepository="kie-server::default::file:$EAP81/repositories/kie/global"

curl -s -u "$KS_USER:$KS_PASS" -X PUT \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0" \
  -d '{"container-id":"example-kjar_1.0.0","release-id":{"group-id":"com.example","artifact-id":"example-kjar","version":"1.0.0"}}'
```

> KIE Server 8.1 is fully backward-compatible with BAMOE 8.0 KJARs — no changes required.

---

## Notes

- **KIE Server managed mode**: BC's Deployment Units UI only works when KIE Server is started with `org.kie.server.controller` pointing at BC. Without it, use the curl deploy command directly.
- **PMML execution** requires `defaultKieSession` (stateful); **DRL** requires `defaultStatelessKieSession`.
- **DMN REST endpoint** requires `default="true"` on `<kbase>` in `kmodule.xml`.
