# ks-integration-tests

Automated JUnit 5 integration tests that exercise all three KIE Server deployment scenarios against a live KIE Server 8.1 instance.

## What is tested

| Test class | Scenario | KJAR built with |
|---|---|---|
| `KsV81IT` | `kjar-v81-to-ks81` | JVM 17 / BAMOE 8.1 (KIE 7.81.1-SNAPSHOT) |
| `KsV80IT` | `kjar-v80-to-ks81` | JVM 11 / BAMOE 8.0 (KIE 7.67.2.Final-redhat-00034) |
| `KsBc80IT` | `kjar-bc80-to-ks81` | JVM 11 / BC 8.0 authored KJAR |

Each test class runs 7 assertions — DMN (2), DRL (2), BPMN (1), PMML (2) — in order using `@BeforeAll` / `@AfterAll` to deploy and undeploy the container.

---

## Prerequisites

**1. KIE Server 8.1 running**

Start the EAP-embedded KIE Server:

```
$EAP81/bin/standalone.sh -c standalone-full.xml
```

Verify it is up:

```
curl -u adminUser:admin@Redhat1 \
  http://localhost:8080/kie-server/services/rest/server
```

**2. KJARs built**

Each scenario's KJAR must be compiled before the integration tests run. Use the JDK that matches each scenario.

For `KsV81IT` — build with JDK 17:

```
cd ../kjar-v81-to-ks81/kjar
mvn clean install
```

For `KsV80IT` — build with JDK 11:

```
cd ../kjar-v80-to-ks81/kjar
mvn clean install
```

For `KsBc80IT` — build with JDK 11:

```
cd ../kjar-bc80-to-ks81/kjar
mvn clean install
```

---

## Running the tests

Integration test classes follow the `*IT.java` naming convention and are excluded from `mvn test`. Activate them with the `it` profile:

```
mvn verify -Pit \
  -DKS_URL=http://localhost:8080/kie-server/services/rest/server \
  -DKS_USER=adminUser \
  -DKS_PASS=admin@Redhat1 \
  -DEAP81=$HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1
```

### Running a single test class

```
mvn verify -Pit -Dit.test=KsV81IT \
  -DKS_URL=http://localhost:8080/kie-server/services/rest/server \
  -DKS_USER=adminUser \
  -DKS_PASS=admin@Redhat1 \
  -DEAP81=$HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1
```

### Skipping integration tests (unit tests only)

```
mvn test
```

---

## Configuration

All connection parameters are read from system properties (`-Dkey=value`) or environment variables. System properties take precedence.

| Parameter | Default | Description |
|---|---|---|
| `KS_URL` | `http://localhost:8080/kie-server/services/rest/server` | KIE Server REST base URL |
| `KS_USER` | `adminUser` | KIE Server username |
| `KS_PASS` | `admin@Redhat1` | KIE Server password |
| `EAP81` | `$HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1` | Path to the EAP 8.1 installation |
| `SCENARIO_ROOT` | Parent directory of `ks-integration-tests/` | Root where all scenario folders live |

---

## How `@BeforeAll` works

Each test class's `@BeforeAll` method does three things:

1. **Copies the KJAR** (`example-kjar-1.0.0.jar` + `pom.xml`) into the KIE Server embedded Maven repository at `$EAP81/repositories/kie/global/com/example/example-kjar/1.0.0/`.
2. **Creates a KIE Server client** using the coordinates above.
3. **Deploys a container** with a unique container ID (e.g. `example-kjar-v81_1.0.0`) so that all three test classes can run against the same server simultaneously without container ID collisions.

`@AfterAll` disposes the container when the test class finishes.

---

## PMML note

`ApplyPmmlModelCommand` is serialised using XStream/JAXB. The PMML tests use a separate `xstreamClient` (marshalling format `XSTREAM`) while all other tests use the JSON client. Both clients connect to the same KIE Server; only the marshalling format differs.

---

## Project structure

```
ks-integration-tests/
  pom.xml
  src/test/java/com/example/it/
    KsTestBase.java       ← shared client setup, KJAR install, container lifecycle
    KsV81IT.java          ← tests for kjar-v81-to-ks81
    KsV80IT.java          ← tests for kjar-v80-to-ks81  (backward-compat)
    KsBc80IT.java         ← tests for kjar-bc80-to-ks81 (backward-compat)
```
