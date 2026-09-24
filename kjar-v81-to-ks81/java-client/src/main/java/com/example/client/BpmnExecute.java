package com.example.client;

import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;

import java.util.Collections;

/**
 * BpmnExecute — starts a HelloProcess instance via KIE Server.
 *
 * Process : com.example.HelloProcess
 * Flow    : Start → Script Task (System.out.println) → End
 * Variables: none required (the script task just prints "Hello BPMN")
 *
 * Expected: KIE Server returns a numeric process instance ID > 0.
 *
 * Run:
 *   mvn exec:java -Dexec.mainClass=com.example.client.BpmnExecute
 */
public class BpmnExecute {

    public static void main(String[] args) {
        KieServicesClient client = KsClient.build();
        ProcessServicesClient process =
            client.getServicesClient(ProcessServicesClient.class);

        System.out.println("\n=== BPMN: HelloProcess ===");
        System.out.println("--- Starting process instance ---");

        // Start the process with no variables (the script task has no inputs)
        Long instanceId = process.startProcess(
            KsClient.CONTAINER_ID,
            KsClient.BPMN_PROCESS,
            Collections.emptyMap());

        System.out.println("Process instance ID = " + instanceId);

        boolean pass = instanceId != null && instanceId > 0;
        System.out.println(pass
            ? "PASS ✓  instance ID=" + instanceId
            : "FAIL ✗  expected instanceId > 0, got: " + instanceId);
    }
}
