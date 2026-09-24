package com.example.client;

/**
 * RunAll — runs all four model clients in sequence.
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass=com.example.client.RunAll
 */
public class RunAll {

    public static void main(String[] args) throws Exception {
        System.out.println("══════════════════════════════════════════════");
        System.out.println("  Scenario 2 — Java Client: All Models");
        System.out.println("══════════════════════════════════════════════");

        DmnExecute.main(args);
        DrlExecute.main(args);
        BpmnExecute.main(args);
        PmmlExecute.main(args);

        System.out.println("\n══════════════════════════════════════════════");
        System.out.println("  Done. Check each model's PASS ✓ / FAIL ✗ above.");
        System.out.println("══════════════════════════════════════════════");
    }
}
