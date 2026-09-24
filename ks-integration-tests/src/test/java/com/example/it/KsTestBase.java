package com.example.it;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Shared base for all KIE Server integration tests.
 *
 * Provides:
 *   - KieServicesClient construction from system properties / env vars
 *   - KJAR installation into the KIE Server embedded Maven repository
 *   - Container deploy / undeploy helpers
 */
public abstract class KsTestBase {

    // ── connection ──────────────────────────────────────────────────────────

    protected static KieServicesClient buildClient(MarshallingFormat format) {
        String url  = prop("KS_URL",  "http://localhost:8080/kie-server/services/rest/server");
        String user = prop("KS_USER", "adminUser");
        String pass = prop("KS_PASS", "admin@Redhat1");

        KieServicesConfiguration cfg =
            KieServicesFactory.newRestConfiguration(url, user, pass);
        cfg.setMarshallingFormat(format);
        return KieServicesFactory.newKieServicesClient(cfg);
    }

    protected static KieServicesClient buildJsonClient() {
        return buildClient(MarshallingFormat.JSON);
    }

    protected static KieServicesClient buildXstreamClient() {
        return buildClient(MarshallingFormat.XSTREAM);
    }

    // ── KJAR installation ───────────────────────────────────────────────────

    /**
     * Copies the KJAR and a POM file into the KIE Server embedded Maven
     * repository so the server can resolve it at container deploy time.
     *
     * @param groupId     Maven groupId  (e.g. "com.example")
     * @param artifactId  Maven artifactId (e.g. "example-kjar")
     * @param version     Maven version  (e.g. "1.0.0")
     * @param jarPath     absolute path to the built JAR
     * @param pomPath     absolute path to the kjar pom.xml
     */
    protected static void installKjar(String groupId, String artifactId, String version,
                                      Path jarPath, Path pomPath) throws IOException {
        String eap81 = prop("EAP81",
            System.getProperty("user.home") + "/BAMOE-8/BAMOE-8.1/jboss-eap-8.1");

        Path repoDir = Paths.get(eap81, "repositories", "kie", "global",
            groupId.replace('.', '/'), artifactId, version);
        Files.createDirectories(repoDir);

        Files.copy(jarPath, repoDir.resolve(artifactId + "-" + version + ".jar"),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(pomPath, repoDir.resolve(artifactId + "-" + version + ".pom"),
            StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Installed KJAR to: " + repoDir);
    }

    // ── container lifecycle ─────────────────────────────────────────────────

    protected static void deployContainer(KieServicesClient client,
                                          String containerId,
                                          String groupId,
                                          String artifactId,
                                          String version) {
        KieContainerResource container = new KieContainerResource(
            containerId,
            new ReleaseId(groupId, artifactId, version));

        ServiceResponse<KieContainerResource> resp =
            client.createContainer(containerId, container);

        if (resp.getType() != ServiceResponse.ResponseType.SUCCESS) {
            throw new IllegalStateException(
                "Failed to deploy container " + containerId + ": " + resp.getMsg());
        }
        System.out.println("Container deployed: " + containerId);
    }

    protected static void undeployContainer(KieServicesClient client, String containerId) {
        ServiceResponse<Void> resp = client.disposeContainer(containerId);
        if (resp.getType() != ServiceResponse.ResponseType.SUCCESS) {
            System.err.println("Warning: could not undeploy container " + containerId
                + " — " + resp.getMsg());
        } else {
            System.out.println("Container undeployed: " + containerId);
        }
    }

    // ── utility ─────────────────────────────────────────────────────────────

    protected static String prop(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : defaultValue;
    }
}
