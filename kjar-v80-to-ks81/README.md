# Migration D — Manual KJAR v8.0 (JVM 11 / BAMOE 8.0) → KIE Server 8.1

Demonstrates building a KJAR **manually** under **JDK 11 / BAMOE 8.0**
(KIE 7.67.2.Final-redhat-00034) and deploying it directly to **KIE Server 8.1**,
then executing all four model types via curl and a Java client.

No Business Central is involved.

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

java-client/                   Maven project — thin Java KIE Server client
  src/main/java/com/example/client/
    KsClient.java               Shared connection factory
    DmnExecute.java             DMN execution demo
    DrlExecute.java             DRL execution demo
    BpmnExecute.java            BPMN execution demo
    PmmlExecute.java            PMML execution demo
    RunAll.java                 Runs all four in sequence
```

---

## Prerequisites

| Requirement    | Version                        |
|----------------|--------------------------------|
| JDK            | 11                             |
| Maven          | 3.6+                           |
| KIE Server 8.1 | running on EAP 8.1             |

Set these variables once in your terminal — every command below uses them:


```bash
    export KS_URL=http://localhost:8080/kie-server/services/rest/server
    export KS_USER=adminUser
    export KS_PASS=admin@Redhat1
```

---

## Step 1 — Build the KJAR locally

```bash
cd kjar
mvn clean install
```

Expected: `BUILD SUCCESS`

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

## Step 2 — Deploy the container to KIE Server 8.1

KIE Server resolves KJARs from its own embedded Maven repository. Copy the
built JAR and POM into the KIE Server repository directory before deploying.

```bash
INSTALL_DIR=$HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1/repositories/kie/global/com/example/manual-v80-kjar/1.0.0
mkdir -p "$INSTALL_DIR"
cp kjar/target/manual-v80-kjar-1.0.0.jar "$INSTALL_DIR/"
cp kjar/pom.xml                           "$INSTALL_DIR/manual-v80-kjar-1.0.0.pom"
```

Then deploy the container:

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/manual-v80-kjar_1.0.0" \
  -d '{
    "container-id": "manual-v80-kjar_1.0.0",
    "release-id": {
      "group-id":    "com.example",
      "artifact-id": "manual-v80-kjar",
      "version":     "1.0.0"
    }
  }'
```

Confirm the container is started:

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -H "Accept: application/json" \
  "$KS_URL/containers/manual-v80-kjar_1.0.0" \
  | grep -o '"status" *: *"[^"]*"' | head -1
```

Expected: `"status" : "STARTED"`

---

## Step 3 — Execute via curl

### DMN — CanDrive.dmn

**Age=25 — expect `Can Drive? = true`**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/manual-v80-kjar_1.0.0/dmn" \
  -d '{"model-namespace":"http://www.example.com/CanDrive","model-name":"CanDrive","dmn-context":{"Age":25}}'
```

Expected: `"Can Drive?": true`

**Age=15 — expect `Can Drive? = false`**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/manual-v80-kjar_1.0.0/dmn" \
  -d '{"model-namespace":"http://www.example.com/CanDrive","model-name":"CanDrive","dmn-context":{"Age":15}}'
```

Expected: `"Can Drive?": false`

---

### DRL — AgeRule.drl

**age=25 — expect `ADULT:25` in results**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/instances/manual-v80-kjar_1.0.0" \
  -d '{"lookup":"defaultStatelessKieSession","commands":[{"set-global":{"identifier":"results","object":{"java.util.ArrayList":[]},"out-identifier":"results"}},{"insert":{"object":{"com.example.Applicant":{"age":25}}}},{"fire-all-rules":{"out-identifier":"fired"}}]}'
```

Expected: `"value": ["ADULT:25"]`

---

### BPMN — HelloProcess.bpmn2

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/manual-v80-kjar_1.0.0/processes/com.example.HelloProcess/instances" \
  -d '{}'
```

Expected: a plain integer process instance ID (e.g. `1`)

---

### PMML — AgeScorecard.pmml

**age=25.0 — expect `score=10.0`**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/instances/manual-v80-kjar_1.0.0" \
  -d '{"lookup":"defaultKieSession","commands":[{"apply-pmml-model-command":{"outIdentifier":"pmml-result","requestData":{"correlationId":"1","modelName":"AgeScorecard","source":"com/example/AgeScorecard.pmml","requestParams":[{"name":"age","type":"java.lang.Double","value":"25.0"}]}}}]}'
```

Expected: `"score": 10.0`

---

## Step 4 — Execute via Java client

```bash
cd java-client
mvn clean package -q
mvn exec:java -Dexec.mainClass=com.example.client.RunAll \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"
```

For individual model execution see
[`../kjar-v81-to-ks81/README.md`](../kjar-v81-to-ks81/README.md)
— same client, substitute `manual-v80-kjar_1.0.0` for the container ID.
