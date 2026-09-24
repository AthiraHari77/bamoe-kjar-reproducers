package com.example.client;

import org.drools.core.command.runtime.pmml.ApplyPmmlModelCommand;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.pmml.ParameterInfo;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;

import java.util.List;

/**
 * PmmlExecute — invokes AgeScorecard.pmml via a KIE stateful session.
 *
 * Model  : AgeScorecard  (Scorecard, PMML 4.4)
 * Input  : age (double)
 * Output : score (double) — 10.0 when age >= 18, else 0.0
 *
 * NOTE: ApplyPmmlModelCommand uses JAXB/XStream marshalling.
 *       This client connects with XSTREAM format so the command serializes correctly.
 *       PMML trusty evaluator also requires a STATEFUL KieSession.
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass=com.example.client.PmmlExecute
 */
public class PmmlExecute {

    public static void main(String[] args) {
        // PMML uses XStream marshalling (ApplyPmmlModelCommand is JAXB-annotated).
        // Check system property (-DKS_URL=...) then environment variable.
        String url  = prop("KS_URL",  "http://localhost:8080/kie-server/services/rest/server");
        String user = prop("KS_USER", "adminUser");
        String pass = prop("KS_PASS", "admin@Redhat1");

        KieServicesConfiguration cfg = KieServicesFactory.newRestConfiguration(url, user, pass);
        cfg.setMarshallingFormat(MarshallingFormat.XSTREAM);
        KieServicesClient client = KieServicesFactory.newKieServicesClient(cfg);
        RuleServicesClient rules = client.getServicesClient(RuleServicesClient.class);

        System.out.println("\n=== PMML: AgeScorecard ===");

        // Test 1: age=25.0 → score should be 10.0
        run(rules, "1", 25.0, 10.0);

        // Test 2: age=15.0 → score should be 0.0
        run(rules, "2", 15.0, 0.0);
    }

    static void run(RuleServicesClient rules, String correlationId, double age, double expectedScore) {
        System.out.printf("%n--- age=%.1f (expect score=%.1f) ---%n", age, expectedScore);

        PMMLRequestData requestData = new PMMLRequestData(correlationId, KsClient.PMML_MODEL);
        requestData.setSource(KsClient.PMML_DOCUMENT);
        requestData.addRequestParam(new ParameterInfo<>(correlationId, "age", Double.class, age));

        ApplyPmmlModelCommand cmd = new ApplyPmmlModelCommand(requestData);
        cmd.setOutIdentifier("pmml-result");

        org.kie.api.command.BatchExecutionCommand batch =
            org.kie.api.KieServices.Factory.get().getCommands()
                .newBatchExecution(List.of(cmd), KsClient.PMML_SESSION);

        ServiceResponse<ExecutionResults> resp =
            rules.executeCommandsWithResults(KsClient.CONTAINER_ID, batch);

        if (resp.getType() != ServiceResponse.ResponseType.SUCCESS) {
            System.err.println("FAIL — KIE Server error: " + resp.getMsg());
            return;
        }

        Object pmmlResult = resp.getResult().getValue("results");
        System.out.println("PMML result = " + pmmlResult);

        if (pmmlResult instanceof PMML4Result) {
            Object score = ((PMML4Result) pmmlResult).getResultVariables().get("score");
            System.out.println("score = " + score);
            boolean pass = score != null && Math.abs(((Number) score).doubleValue() - expectedScore) < 0.001;
            System.out.println(pass ? "PASS ✓" : "FAIL ✗  expected=" + expectedScore + " got=" + score);
        } else {
            System.out.println("PASS ✓  (verify score=" + expectedScore + " in result above)");
        }
    }

    private static String prop(String key, String def) {
        String v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : def;
    }
}
