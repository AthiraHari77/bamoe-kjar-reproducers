package com.example.client;

import org.kie.server.client.KieServicesClient;

/**
 * RunAll — deploys the container then runs all four model clients in sequence.
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass=com.example.client.RunAll \
 *     -DEAP81="$EAP81" -DPROJECT="$PROJECT"
 */
public class RunAll {

    public static void main(String[] args) throws Exception {
        System.out.println("══════════════════════════════════════════════");
        System.out.println("  kjar-v80-to-ks81 — Java Client: Deploy + All Models");
        System.out.println("══════════════════════════════════════════════");

        // Step 1: install KJAR into KIE Server repo and deploy the container
        KieServicesClient client = KsClient.build();
        DeployContainer.installAndDeploy(client);

        // Step 2: execute all four model types
        DmnExecute.main(args);
        DrlExecute.main(args);
        BpmnExecute.main(args);
        PmmlExecute.main(args);

        System.out.println("\n══════════════════════════════════════════════");
        System.out.println("  Done. Check each model's PASS ✓ / FAIL ✗ above.");
        System.out.println("══════════════════════════════════════════════");
    }
}
