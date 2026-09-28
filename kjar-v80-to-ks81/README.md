# Migration D — Manual KJAR (JDK 11 / BAMOE 8.0) → KIE Server 8.1

Build a KJAR manually under **JDK 11 / BAMOE 8.0** (KIE 7.67.x), then deploy
and execute on **KIE Server 8.1**. KIE Server 8.1 is fully backward-compatible —
no changes to the KJAR are required.

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

| Requirement    | Version                    |
|----------------|----------------------------|
| JDK            | 11                         |
| Maven          | 3.6+                       |
| KIE Server 8.1 | running                    |

```bash
export KS_URL=http://localhost:8080/kie-server/services/rest/server
export KS_USER=adminUser
export KS_PASS=admin@Redhat1
export EAP81=$HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1
export PROJECT=$(pwd)
```

---

## Step 1 — Build and install the KJAR (with JDK 11)

```bash
cd "$PROJECT/kjar"
mvn clean deploy -DskipTests \
  -DaltDeploymentRepository="kie-server::default::file:$EAP81/repositories/kie/global"
```

Expected: `BUILD SUCCESS` — Maven lays out the JAR and POM in the KIE Server repo directly.

---

## Step 2 — Deploy the container

### Option A — via curl

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
  -DEAP81="$EAP81" -DPROJECT="$PROJECT" \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"
```

Expected:
```
Container deployed: example-kjar_1.0.0  status=STARTED
```

---

## Step 3 — Execute via curl

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

## Step 4 — Execute via Java client

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

For expected output and technical notes see [`../kjar-v81-to-ks81/README.md`](../kjar-v81-to-ks81/README.md).
