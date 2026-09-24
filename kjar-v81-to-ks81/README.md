# Scenario 2 — Manual KJAR (JVM 17 / BAMOE 8.1) → KIE Server 8.1

Demonstrates building a KJAR manually under **JDK 17 / BAMOE 8.1** and
deploying it directly to **KIE Server 8.1**, then executing all four model
types (DMN, DRL, BPMN, PMML) via **curl** and a **Java client**.

No Business Central is involved.

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

java-client/                 Maven project — thin Java KIE Server client
  src/main/java/com/example/client/
    KsClient.java             Shared connection factory
    DmnExecute.java           DMN execution demo
    DrlExecute.java           DRL execution demo
    BpmnExecute.java          BPMN execution demo
    PmmlExecute.java          PMML execution demo
    RunAll.java               Runs all four in sequence
```

---

## Prerequisites

| Requirement    | Version            |
|----------------|--------------------|
| JDK            | 17                 |
| Maven          | 3.8+               |
| KIE Server 8.1 | running on EAP 8.1 |

Set these variables once in your terminal — every command below uses them.
Run these from inside the `kjar-v81-to-ks81/` directory:

```bash
    export KS_URL=http://localhost:8080/kie-server/services/rest/server
    export KS_USER=adminUser
    export KS_PASS=admin@Redhat1
    export EAP81=$HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1
    export PROJECT=$(pwd)
```

---

## Step 1 — Build the KJAR

```bash
cd "$PROJECT/kjar"
mvn clean install
```

Expected: `BUILD SUCCESS`

The `kie-maven-plugin` validates all four model files, compiles the DRL rules
into a KieBase cache, and packages everything into
`target/example-kjar-1.0.0.jar`.

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

## Step 2 — Install the KJAR into the KIE Server Maven repository

KIE Server resolves KJARs from its own embedded Maven repository, **not**
from `~/.m2`. Copy the JAR and the POM there so the server can load it at
container deploy time.

```bash
INSTALL_DIR="$EAP81/repositories/kie/global/com/example/example-kjar/1.0.0"
mkdir -p "$INSTALL_DIR"

cp "$PROJECT/kjar/target/example-kjar-1.0.0.jar" "$INSTALL_DIR/"
cp "$PROJECT/kjar/pom.xml"                       "$INSTALL_DIR/example-kjar-1.0.0.pom"
```

Confirm:

```bash
ls -lh "$INSTALL_DIR"
```

Expected files:

```
example-kjar-1.0.0.jar
example-kjar-1.0.0.pom
```

---

## Step 3 — Verify KIE Server is running

```bash
curl -s -u "$KS_USER:$KS_PASS" "$KS_URL" | grep -o '"version":"[^"]*"'
```

Expected: `"version":"7.81.1-SNAPSHOT"`

---

## Step 4 — Deploy the container

If the container already exists from a previous run, delete it first:

```bash
curl -s -u "$KS_USER:$KS_PASS" -X DELETE "$KS_URL/containers/example-kjar_1.0.0"
```

Deploy:

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

Expected response contains `"type":"SUCCESS"` and `"msg":"Container example-kjar_1.0.0 successfully deployed..."`.

Confirm the container is `STARTED`:

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0" \
  | grep -o '"status" *: *"[^"]*"' | head -1
```

Expected: `"status" : "STARTED"`

---

## Step 5 — Execute via curl

### DMN — CanDrive.dmn

The DMN model evaluates whether a person can drive based on age (>= 18).

**Test 1: Age=25 — expect `Can Drive? = true`**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/dmn" \
  -d '{
    "model-namespace": "http://www.example.com/CanDrive",
    "model-name":      "CanDrive",
    "dmn-context":     {"Age": 25}
  }'
```

Expected: `"Can Drive?": true`

**Test 2: Age=15 — expect `Can Drive? = false`**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/dmn" \
  -d '{
    "model-namespace": "http://www.example.com/CanDrive",
    "model-name":      "CanDrive",
    "dmn-context":     {"Age": 15}
  }'
```

Expected: `"Can Drive?": false`

---

### DRL — AgeRule.drl

The DRL rules fire on `Applicant` facts and append a string to the `results` list.

**Test 1: age=25 — expect `ADULT:25` in results**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{
    "lookup": "defaultStatelessKieSession",
    "commands": [
      {"set-global": {"identifier": "results",
                      "object": {"java.util.ArrayList": []},
                      "out-identifier": "results"}},
      {"insert":     {"object": {"com.example.Applicant": {"age": 25}}}},
      {"fire-all-rules": {"out-identifier": "fired"}}
    ]
  }'
```

Expected: `"value": ["ADULT:25"]` in results

**Test 2: age=15 — expect `MINOR:15` in results**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{
    "lookup": "defaultStatelessKieSession",
    "commands": [
      {"set-global": {"identifier": "results",
                      "object": {"java.util.ArrayList": []},
                      "out-identifier": "results"}},
      {"insert":     {"object": {"com.example.Applicant": {"age": 15}}}},
      {"fire-all-rules": {"out-identifier": "fired"}}
    ]
  }'
```

Expected: `"value": ["MINOR:15"]` in results

---

### BPMN — HelloProcess.bpmn2

Starts the `com.example.HelloProcess` process. The response is the numeric
process instance ID.

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/example-kjar_1.0.0/processes/com.example.HelloProcess/instances" \
  -d '{}'
```

Expected: a plain integer (e.g. `1`)

---

### PMML — AgeScorecard.pmml

The PMML Scorecard model maps age to a score: age ≥ 18 → 10.0, else 0.0.

> **Note:** `apply-pmml-model-command` uses JAXB/XStream serialisation.
> The `Content-Type` and `Accept` headers are `application/xml` for this endpoint.
> The `requestParams` value must be sent as a **string** — the server coerces it to `Double`.

**Test 1: age=25.0 — expect `score=10.0`**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{
    "lookup": "defaultKieSession",
    "commands": [{
      "apply-pmml-model-command": {
        "outIdentifier": "pmml-result",
        "requestData": {
          "correlationId": "1",
          "modelName":     "AgeScorecard",
          "source":        "com/example/AgeScorecard.pmml",
          "requestParams": [
            {"name": "age", "type": "java.lang.Double", "value": "25.0"}
          ]
        }
      }
    }]
  }'
```

Expected: `"score": 10.0` inside `resultVariables`

**Test 2: age=15.0 — expect `score=0.0`**

```bash
curl -s -u "$KS_USER:$KS_PASS" \
  -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  "$KS_URL/containers/instances/example-kjar_1.0.0" \
  -d '{
    "lookup": "defaultKieSession",
    "commands": [{
      "apply-pmml-model-command": {
        "outIdentifier": "pmml-result",
        "requestData": {
          "correlationId": "2",
          "modelName":     "AgeScorecard",
          "source":        "com/example/AgeScorecard.pmml",
          "requestParams": [
            {"name": "age", "type": "java.lang.Double", "value": "15.0"}
          ]
        }
      }
    }]
  }'
```

Expected: `"score": 0.0` inside `resultVariables`

---

## Step 6 — Execute via Java client

Build the client:

```bash
cd "$PROJECT/java-client"
mvn clean package -q
```

Run each model type individually:

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

# PMML  (uses XStream marshalling — creates its own connection with MarshallingFormat.XSTREAM)
mvn exec:java -Dexec.mainClass=com.example.client.PmmlExecute \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"

# All at once
mvn exec:java -Dexec.mainClass=com.example.client.RunAll \
  -DKS_URL="$KS_URL" -DKS_USER="$KS_USER" -DKS_PASS="$KS_PASS"
```

> **Note:** The Java client reads `KS_URL`, `KS_USER`, `KS_PASS` from environment
> variables. The `-D` flags above set them as system properties — the client checks
> both. You can also just `export` them before running.

Expected output (all four models):

```
=== DMN: CanDrive ===
--- Age=25 (expect Can Drive? = true) ---
Can Drive? = true
PASS ✓
--- Age=15 (expect Can Drive? = false) ---
Can Drive? = false
PASS ✓

=== DRL: AgeRule ===
--- age=25 (expect ADULT:25 in results) ---
Rules fired = 1
Results     = [ADULT:25]
PASS ✓
--- age=15 (expect MINOR:15 in results) ---
Rules fired = 1
Results     = [MINOR:15]
PASS ✓

=== BPMN: HelloProcess ===
--- Starting process instance ---
Process instance ID = 1
PASS ✓  instance ID=1

=== PMML: AgeScorecard ===
--- age=25.0 (expect score=10.0) ---
score = 10.0
PASS ✓
--- age=15.0 (expect score=0.0) ---
score = 0.0
PASS ✓
```

---

## Notes

### Why `defaultKieSession` for PMML (not `defaultStatelessKieSession`)

`ApplyPmmlModelCommand` uses the PMML trusty evaluator, which needs to resolve
the `KieBase` classloader at execution time. A stateless session does not hold a
`KieSession` reference in the registry context, so the classloader lookup returns
null. The stateful `defaultKieSession` keeps the context alive for the duration
of the command.

### Why `MarshallingFormat.XSTREAM` for the Java PMML client

`ApplyPmmlModelCommand` and `PMMLRequestData` are annotated with JAXB/XStream
annotations (`@XmlRootElement`, `@XmlAttribute`). The default JSON marshaller
(Jackson) cannot serialize the generic `ParameterInfo<T>.value` field. Switching
the client connection to `MarshallingFormat.XSTREAM` lets the command serialize
correctly over the wire.

### Why `default="true"` on `<kbase>` in kmodule.xml

The KIE Server DMN REST endpoint (`/containers/{id}/dmn`) resolves the model
through the container's default KieBase. Without `default="true"` on the
`<kbase>` element, the endpoint returns `Cannot find a default KieBase` even
though the DMN file is present in the KJAR.

### Maven dependency notes

The `java-client/pom.xml` pins `jackson-annotations`, `jackson-core`, and
`jackson-databind` to **2.17.2**. The `kie-server-client` transitively resolves
Jackson 2.22, which introduces `@JsonSerializeAs` — an annotation that does not
exist in older Jackson JARs cached in `~/.m2`. Pinning ensures a consistent
version is used at compile and runtime.
