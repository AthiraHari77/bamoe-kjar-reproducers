package com.example.client;

import com.example.Applicant;
import org.kie.api.KieServices;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.RuleServicesClient;

import java.util.ArrayList;
import java.util.List;

/**
 * DrlExecute — fires AgeRule.drl rules via a stateless KIE session.
 *
 * Rules  : Adult (age >= 18) and Minor (age < 18)
 * Output : global "results" list — e.g. ["ADULT:25"] or ["MINOR:15"]
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass=com.example.client.DrlExecute
 */
public class DrlExecute {

    public static void main(String[] args) {
        KieServicesClient client = KsClient.build();
        RuleServicesClient rules = client.getServicesClient(RuleServicesClient.class);

        System.out.println("\n=== DRL: AgeRule ===");

        // Test 1: age=25 → Adult rule fires
        run(rules, 25, "ADULT:25");

        // Test 2: age=15 → Minor rule fires
        run(rules, 15, "MINOR:15");
    }

    static void run(RuleServicesClient rules, int age, String expectedInResults) {
        System.out.printf("%n--- age=%d (expect %s in results) ---%n", age, expectedInResults);

        KieCommands cf = KieServices.Factory.get().getCommands();
        List<Command<?>> cmds = new ArrayList<>();

        // Initialise the global "results" list — must be done before fire
        cmds.add(cf.newSetGlobal("results", new ArrayList<>(), "results"));
        // Insert an Applicant fact (the DRL pattern-matches on Applicant.age)
        cmds.add(cf.newInsert(new Applicant(age)));
        // Fire all matching rules and capture the count
        cmds.add(cf.newFireAllRules("fired"));

        BatchExecutionCommand batch = cf.newBatchExecution(cmds, KsClient.DRL_SESSION);
        ServiceResponse<ExecutionResults> resp =
            rules.executeCommandsWithResults(KsClient.CONTAINER_ID, batch);

        if (resp.getType() != ServiceResponse.ResponseType.SUCCESS) {
            System.err.println("FAIL — KIE Server error: " + resp.getMsg());
            return;
        }

        int fired    = ((Number) resp.getResult().getValue("fired")).intValue();
        Object results = resp.getResult().getValue("results");

        System.out.println("Rules fired = " + fired);
        System.out.println("Results     = " + results);

        boolean pass = fired == 1 && results.toString().contains(expectedInResults);
        System.out.println(pass ? "PASS ✓" : "FAIL ✗  expected fired=1 and '" + expectedInResults + "' in results");
    }
}
