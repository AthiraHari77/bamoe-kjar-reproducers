package com.example.client;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

/**
 * KsClient — shared KIE Server connection factory and KJAR coordinates.
 *
 * Reads KS_URL / KS_USER / KS_PASS from system properties (-D flags) first,
 * then environment variables, then falls back to localhost defaults.
 */
public class KsClient {

    // ── KJAR Maven coordinates ──────────────────────────────────────────────
    public static final String GROUP_ID      = "com.example";
    public static final String ARTIFACT_ID   = "example-kjar";
    public static final String VERSION       = "1.0.0";

    // ── KIE Server container ────────────────────────────────────────────────
    public static final String CONTAINER_ID  = "example-kjar_1.0.0";

    // ── Model references ────────────────────────────────────────────────────
    public static final String DMN_NAMESPACE = "http://www.example.com/CanDrive";
    public static final String DMN_MODEL     = "CanDrive";
    public static final String DRL_SESSION   = "defaultStatelessKieSession";
    public static final String PMML_SESSION  = "defaultKieSession";   // PMML needs stateful
    public static final String BPMN_PROCESS  = "com.example.HelloProcess";
    public static final String PMML_DOCUMENT = "com/example/AgeScorecard.pmml";
    public static final String PMML_MODEL    = "AgeScorecard";

    /** Create and return a connected KieServicesClient (JSON marshalling). */
    public static KieServicesClient build() {
        String url  = env("KS_URL",  "http://localhost:8080/kie-server/services/rest/server");
        String user = env("KS_USER", "adminUser");
        String pass = env("KS_PASS", "admin@Redhat1");

        System.out.println("Connecting to KIE Server: " + url);

        KieServicesConfiguration cfg =
            KieServicesFactory.newRestConfiguration(url, user, pass);
        cfg.setMarshallingFormat(MarshallingFormat.JSON);

        return KieServicesFactory.newKieServicesClient(cfg);
    }

    static String env(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }
}
