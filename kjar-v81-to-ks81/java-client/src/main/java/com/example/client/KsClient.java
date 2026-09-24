package com.example.client;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

/**
 * Shared KIE Server connection setup.
 * Reads configuration from environment variables so you can point at
 * any server without recompiling.
 *
 * Defaults (for local single-machine setup):
 *   KS_URL  = http://localhost:8080/kie-server/services/rest/server
 *   KS_USER = adminUser
 *   KS_PASS = admin@Redhat1
 */
public class KsClient {

    public static final String CONTAINER_ID  = "example-kjar_1.0.0";
    public static final String DMN_NAMESPACE = "http://www.example.com/CanDrive";
    public static final String DMN_MODEL     = "CanDrive";
    public static final String DRL_SESSION   = "defaultStatelessKieSession";
    public static final String PMML_SESSION  = "defaultKieSession";  // PMML needs stateful
    public static final String BPMN_PROCESS  = "com.example.HelloProcess";
    public static final String PMML_DOCUMENT = "com/example/AgeScorecard.pmml";
    public static final String PMML_MODEL    = "AgeScorecard";

    /** Create and return a connected KieServicesClient. */
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

    private static String env(String key, String defaultValue) {
        // Check system property first (-Dkey=value), then environment variable
        String v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }
}
