# Scenario A — BC 8.1 KJAR → KIE Server 8.1

Author all four model types in **Business Central 8.1**, verify with Scenario Simulations,
download the KJAR, then deploy and execute on **KIE Server 8.1**.

---

## What is in this folder

```
kjar/                          Represents the KJAR exported from Business Central 8.1
  src/main/resources/com/example/
    CanDrive.dmn                DMN — "Can Drive?" decision  (Age >= 18)
    AgeRule.drl                 DRL — Adult / Minor classification
    HelloProcess.bpmn2          BPMN2 — single script task
    AgeScorecard.pmml           PMML Scorecard — age → score
  src/test/resources/com/example/
    CanDrive.scesim             Scenario Simulation for DMN
    AgeRule.scesim              Scenario Simulation for DRL

java-client/                   Java KIE Server client
  src/main/java/com/example/client/
    KsClient.java               Connection factory + Maven coordinates
    DeployContainer.java        Installs KJAR into KIE Server repo and deploys container
    DmnExecute.java             DMN execution
    DrlExecute.java             DRL execution
    BpmnExecute.java            BPMN execution
    PmmlExecute.java            PMML execution
    RunAll.java                 Deploy + all four models in one command
```

---

## Prerequisites

| Requirement          | Version |
|----------------------|---------|
| JDK                  | 17      |
| Maven                | 3.8+    |
| Business Central 8.1 | running |
| KIE Server 8.1       | running |

```bash
export KS_URL=http://localhost:8080/kie-server/services/rest/server
export KS_USER=<kie-server-user>
export KS_PASS=<kie-server-password>
export EAP81=<path-to-jboss-eap-8.1>
export PROJECT=$(pwd)
```

---

## Step 1 — Author assets in Business Central 8.1

In BC 8.1 (`http://localhost:8080/business-central`), **Design → Add Project**: name `example-kjar`, group `com.example`, artifact `example-kjar`, version `1.0.0`.

Add these assets (all in package `com.example`):

| Asset          | Type             | Details                                                                                 |
|----------------|------------------|-----------------------------------------------------------------------------------------|
| `CanDrive`     | DMN              | Input `Age` (number) → decision `Can Drive?` (boolean): `Age >= 18`                     |
| `Applicant`    | Data Object      | Field `age` (Integer)                                                                   |
| `AgeRule`      | DRL              | See `kjar/src/main/resources/com/example/AgeRule.drl`                                   |
| `HelloProcess` | Business Process | Script task: `System.out.println("Hello BPMN");`, process ID `com.example.HelloProcess` |
| `AgeScorecard` | PMML             | Upload `kjar/src/main/resources/com/example/AgeScorecard.pmml`                          |

---

## Step 2 — Run Scenario Simulations

| Simulation  | Type  | Rows                               | Expected                   |
|-------------|-------|------------------------------------|----------------------------|
| `CanDrive`  | DMN   | Age=25, Age=15                     | `Can Drive?` = true, false |
| `AgeRule`   | Rule  | Applicant.age=25, Applicant.age=15 | age round-trips unchanged  |

**Add Asset → Test Scenario**, run each with **▶ Run** — all rows must pass (green).

---

## Step 3 — Download the KJAR

1. Go to **Build → Build & Download**
2. Note the full path and export it:
   ```bash
   export JAR_PATH="/Users/you/Downloads/example-kjar-1.0.0.jar"
   ```

---

## Step 4 — Install into KIE Server repo + deploy container

### Option A — via Maven + curl

Install with Maven (handles JAR layout and POM generation automatically):

```bash
mvn install:install-file \
  -Dfile="$JAR_PATH" \
  -DgroupId=com.example \
  -DartifactId=example-kjar \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DlocalRepositoryPath="$EAP81/repositories/kie/global"
```

Then deploy the container:

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0" \
  -d '{
    "container-id": "example-kjar_1.0.0",
    "release-id": {
      "group-id":    "com.example",
      "artifact-id": "example-kjar",
      "version":     "1.0.0"
    }
  }'
```

Expected response contains `"type":"SUCCESS"`.

### Option B — via Java client

```bash
cd "$PROJECT/java-client"
mvn compile exec:java -Dexec.mainClass=com.example.client.DeployContainer \
  -DEAP81="$EAP81" -DJAR_PATH="$JAR_PATH" \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"
```

Expected:
```
Container deployed: example-kjar_1.0.0  status=STARTED
```

---

## Step 5 — Execute via curl

### DMN

```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/dmn" \
  -d '{"model-namespace":"http://www.example.com/CanDrive","model-name":"CanDrive","dmn-context":{"Age":25}}'
```
Expected: `"Can Drive?": true`

```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/dmn" \
  -d '{"model-namespace":"http://www.example.com/CanDrive","model-name":"CanDrive","dmn-context":{"Age":15}}'
```
Expected: `"Can Drive?": false`

### DRL

```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{"lookup":"defaultStatelessKieSession","commands":[{"set-global":{"identifier":"results","object":{"java.util.ArrayList":[]},"out-identifier":"results"}},{"insert":{"object":{"com.example.Applicant":{"age":25}}}},{"fire-all-rules":{"out-identifier":"fired"}}]}'
```
Expected: `"value": ["ADULT:25"]`

### BPMN

```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/processes/com.example.HelloProcess/instances" \
  -d '{}'
```
Expected: a numeric process instance ID (e.g. `1`)

### PMML

```bash
curl -s -u "$KS_USER:$KS_PASS" -X POST \
  -H "Content-Type: application/json" -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{"lookup":"defaultKieSession","commands":[{"apply-pmml-model-command":{"outIdentifier":"pmml-result","requestData":{"correlationId":"1","modelName":"AgeScorecard","source":"com/example/AgeScorecard.pmml","requestParams":[{"name":"age","type":"java.lang.Double","value":"25.0"}]}}}]}'
```
Expected: `"score": 10.0`

---

## Step 6 — Execute via Java client

```bash
cd "$PROJECT/java-client"
mvn clean package -q
```

```bash
# DMN
mvn exec:java -Dexec.mainClass=com.example.client.DmnExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

# DRL
mvn exec:java -Dexec.mainClass=com.example.client.DrlExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

# BPMN
mvn exec:java -Dexec.mainClass=com.example.client.BpmnExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

# PMML
mvn exec:java -Dexec.mainClass=com.example.client.PmmlExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

# Deploy + all four in one command
mvn exec:java -Dexec.mainClass=com.example.client.RunAll \
  -DEAP81="$EAP81" -DPROJECT="$PROJECT" \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"
```

---

## Notes

- PMML execution uses `MarshallingFormat.XSTREAM` — `PmmlExecute` creates its own connection automatically
- `RunAll` calls `DeployContainer` first, so it redeploys the container before running all four models
- For KIE Server technical notes (KieBase default, XStream PMML, session types) see [`../kjar-v81-to-ks81/README.md`](../kjar-v81-to-ks81/README.md)
