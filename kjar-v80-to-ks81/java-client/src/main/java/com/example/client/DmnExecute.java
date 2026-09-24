package com.example.client;

import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNDecisionResult;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.core.impl.DMNContextImpl;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.DMNServicesClient;
import org.kie.server.client.KieServicesClient;

/**
 * DmnExecute — evaluates CanDrive.dmn via KIE Server.
 *
 * Model   : CanDrive
 * Input   : Age (number)
 * Output  : Can Drive? (boolean) — true when Age >= 18
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass=com.example.client.DmnExecute
 */
public class DmnExecute {

    public static void main(String[] args) {
        KieServicesClient client = KsClient.build();
        DMNServicesClient dmn = client.getServicesClient(DMNServicesClient.class);

        System.out.println("\n=== DMN: CanDrive ===");

        // Test 1: Age=25 — should be able to drive
        run(dmn, 25, true);

        // Test 2: Age=15 — should NOT be able to drive
        run(dmn, 15, false);
    }

    static void run(DMNServicesClient dmn, int age, boolean expected) {
        System.out.printf("%n--- Age=%d (expect Can Drive? = %b) ---%n", age, expected);

        DMNContext ctx = new DMNContextImpl();
        ctx.set("Age", age);

        ServiceResponse<DMNResult> resp =
            dmn.evaluateAll(KsClient.CONTAINER_ID, KsClient.DMN_NAMESPACE, KsClient.DMN_MODEL, ctx);

        if (resp.getType() != ServiceResponse.ResponseType.SUCCESS) {
            System.err.println("FAIL — KIE Server error: " + resp.getMsg());
            return;
        }

        DMNDecisionResult decisionResult = resp.getResult().getDecisionResultByName("Can Drive?");
        Object result = decisionResult != null ? decisionResult.getResult() : null;

        System.out.println("Can Drive? = " + result);

        boolean actual = Boolean.TRUE.equals(result);
        System.out.println(actual == expected ? "PASS ✓" : "FAIL ✗  expected=" + expected);
    }
}
