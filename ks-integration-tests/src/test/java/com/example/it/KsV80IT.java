package com.example.it;

import com.example.Applicant;
import org.drools.core.command.runtime.pmml.ApplyPmmlModelCommand;
import org.junit.jupiter.api.*;
import org.kie.api.KieServices;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.pmml.ParameterInfo;
import org.kie.api.runtime.ExecutionResults;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.core.impl.DMNContextImpl;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KsV80IT — Integration tests for the kjar-v80-to-ks81 scenario.
 *
 * KJAR: example-kjar 1.0.0, built with JVM 11 / BAMOE 8.0 (KIE 7.67.2.Final-redhat-00034)
 * Target: KIE Server 8.1
 *
 * Validates backward-compatibility: a KJAR compiled against BAMOE 8.0 runs
 * unchanged on KIE Server 8.1.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("kjar-v80 (BAMOE 8.0) → KIE Server 8.1  [backward-compat]")
public class KsV80IT extends KsTestBase {

    static final String CONTAINER_ID  = "example-kjar-v80_1.0.0";
    static final String GROUP_ID      = "com.example";
    static final String ARTIFACT_ID   = "example-kjar";
    static final String VERSION       = "1.0.0";
    static final String DMN_NAMESPACE = "http://www.example.com/CanDrive";
    static final String DMN_MODEL     = "CanDrive";
    static final String DRL_SESSION   = "defaultStatelessKieSession";
    static final String PMML_SESSION  = "defaultKieSession";
    static final String BPMN_PROCESS  = "com.example.HelloProcess";
    static final String PMML_DOCUMENT = "com/example/AgeScorecard.pmml";
    static final String PMML_MODEL    = "AgeScorecard";

    static KieServicesClient jsonClient;
    static KieServicesClient xstreamClient;

    @BeforeAll
    static void deployKjar() throws Exception {
        String scenarioRoot = prop("SCENARIO_ROOT",
            Paths.get("").toAbsolutePath().getParent().toString());
        Path jarPath = Paths.get(scenarioRoot,
            "kjar-v80-to-ks81", "kjar", "target", "example-kjar-1.0.0.jar");
        Path pomPath = Paths.get(scenarioRoot,
            "kjar-v80-to-ks81", "kjar", "pom.xml");

        assertTrue(jarPath.toFile().exists(),
            "KJAR not found at " + jarPath + " — run 'mvn clean install' in kjar-v80-to-ks81/kjar first");

        installKjar(GROUP_ID, ARTIFACT_ID, VERSION, jarPath, pomPath);

        jsonClient    = buildJsonClient();
        xstreamClient = buildXstreamClient();

        deployContainer(jsonClient, CONTAINER_ID, GROUP_ID, ARTIFACT_ID, VERSION);
    }

    @AfterAll
    static void undeployKjar() {
        if (jsonClient != null) {
            undeployContainer(jsonClient, CONTAINER_ID);
        }
    }

    @Test @Order(1)
    @DisplayName("DMN CanDrive: age=25 → Can Drive?=true")
    void dmnAge25CanDrive() {
        DMNServicesClient dmn = jsonClient.getServicesClient(DMNServicesClient.class);
        DMNContext ctx = new DMNContextImpl();
        ctx.set("Age", 25);

        ServiceResponse<DMNResult> resp =
            dmn.evaluateAll(CONTAINER_ID, DMN_NAMESPACE, DMN_MODEL, ctx);

        assertEquals(ServiceResponse.ResponseType.SUCCESS, resp.getType(), resp.getMsg());
        DMNDecisionResult decision = resp.getResult().getDecisionResultByName("Can Drive?");
        assertNotNull(decision);
        assertEquals(Boolean.TRUE, decision.getResult());
    }

    @Test @Order(2)
    @DisplayName("DMN CanDrive: age=15 → Can Drive?=false")
    void dmnAge15CannotDrive() {
        DMNServicesClient dmn = jsonClient.getServicesClient(DMNServicesClient.class);
        DMNContext ctx = new DMNContextImpl();
        ctx.set("Age", 15);

        ServiceResponse<DMNResult> resp =
            dmn.evaluateAll(CONTAINER_ID, DMN_NAMESPACE, DMN_MODEL, ctx);

        assertEquals(ServiceResponse.ResponseType.SUCCESS, resp.getType(), resp.getMsg());
        assertEquals(Boolean.FALSE,
            resp.getResult().getDecisionResultByName("Can Drive?").getResult());
    }

    @Test @Order(3)
    @DisplayName("DRL AgeRule: age=25 → ADULT:25")
    void drlAge25Adult() {
        RuleServicesClient rules = jsonClient.getServicesClient(RuleServicesClient.class);
        ServiceResponse<ExecutionResults> resp = fireDrlRules(rules, 25);

        assertEquals(ServiceResponse.ResponseType.SUCCESS, resp.getType(), resp.getMsg());
        assertEquals(1, ((Number) resp.getResult().getValue("fired")).intValue());
        assertTrue(resp.getResult().getValue("results").toString().contains("ADULT:25"));
    }

    @Test @Order(4)
    @DisplayName("DRL AgeRule: age=15 → MINOR:15")
    void drlAge15Minor() {
        RuleServicesClient rules = jsonClient.getServicesClient(RuleServicesClient.class);
        ServiceResponse<ExecutionResults> resp = fireDrlRules(rules, 15);

        assertEquals(ServiceResponse.ResponseType.SUCCESS, resp.getType(), resp.getMsg());
        assertTrue(resp.getResult().getValue("results").toString().contains("MINOR:15"));
    }

    @Test @Order(5)
    @DisplayName("BPMN HelloProcess: starts and returns instanceId > 0")
    void bpmnStartProcess() {
        ProcessServicesClient process = jsonClient.getServicesClient(ProcessServicesClient.class);
        Long instanceId = process.startProcess(CONTAINER_ID, BPMN_PROCESS, Collections.emptyMap());
        assertNotNull(instanceId);
        assertTrue(instanceId > 0);
    }

    @Test @Order(6)
    @DisplayName("PMML AgeScorecard: age=25.0 → score=10.0")
    void pmmlAge25Score10() {
        assertEquals(10.0, runPmml("1", 25.0), 0.001);
    }

    @Test @Order(7)
    @DisplayName("PMML AgeScorecard: age=15.0 → score=0.0")
    void pmmlAge15Score0() {
        assertEquals(0.0, runPmml("2", 15.0), 0.001);
    }

    private ServiceResponse<ExecutionResults> fireDrlRules(RuleServicesClient rules, int age) {
        KieCommands cf = KieServices.Factory.get().getCommands();
        List<Command<?>> cmds = new ArrayList<>();
        cmds.add(cf.newSetGlobal("results", new ArrayList<>(), "results"));
        cmds.add(cf.newInsert(new Applicant(age)));
        cmds.add(cf.newFireAllRules("fired"));
        return rules.executeCommandsWithResults(CONTAINER_ID,
            cf.newBatchExecution(cmds, DRL_SESSION));
    }

    private double runPmml(String correlationId, double age) {
        RuleServicesClient rules = xstreamClient.getServicesClient(RuleServicesClient.class);
        PMMLRequestData req = new PMMLRequestData(correlationId, PMML_MODEL);
        req.setSource(PMML_DOCUMENT);
        req.addRequestParam(new ParameterInfo<>(correlationId, "age", Double.class, age));

        ApplyPmmlModelCommand cmd = new ApplyPmmlModelCommand(req);
        cmd.setOutIdentifier("pmml-result");

        ServiceResponse<ExecutionResults> resp = rules.executeCommandsWithResults(CONTAINER_ID,
            KieServices.Factory.get().getCommands().newBatchExecution(List.of(cmd), PMML_SESSION));

        assertEquals(ServiceResponse.ResponseType.SUCCESS, resp.getType(), resp.getMsg());
        Object result = resp.getResult().getValue("results");
        assertInstanceOf(PMML4Result.class, result);
        Object score = ((PMML4Result) result).getResultVariables().get("score");
        assertNotNull(score);
        return ((Number) score).doubleValue();
    }
}
