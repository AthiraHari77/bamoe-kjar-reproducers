package com.example.client;

import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * DeployContainer — installs the KJAR into the KIE Server embedded Maven
 * repository and deploys (or redeploys) the container.
 *
 * Two steps are required because KIE Server 8.1 resolves KJARs from its own
 * embedded Maven repository under EAP's data directory — NOT from ~/.m2.
 *
 * Step 1: publish the KJAR to the KIE Server Maven repo via Maven.
 *   - Normal build: runs 'mvn deploy -DaltDeploymentRepository=...' in kjar/
 *   - Downloaded JAR: runs 'mvn install:install-file' against the given file
 *
 * Step 2: call KieServicesClient.createContainer() to deploy the container.
 *
 * Configuration (system property or env var, with defaults):
 *   EAP81    — path to jboss-eap-8.1 home
 *              default: $HOME/BAMOE-8/BAMOE-8.1/jboss-eap-8.1
 *   PROJECT  — path to this scenario's root folder (parent of java-client/)
 *              default: parent of the current working directory
 *   JAR_PATH — full path to an already-built/downloaded KJAR (optional)
 *              when set, uses 'mvn install:install-file' instead of deploy
 *
 * Run (from java-client/ directory):
 *   mvn exec:java -Dexec.mainClass=com.example.client.DeployContainer \
 *     -DEAP81="$EAP81" -DPROJECT="$PROJECT"
 *
 *   # or with a downloaded JAR:
 *   mvn exec:java -Dexec.mainClass=com.example.client.DeployContainer \
 *     -DEAP81="$EAP81" -DJAR_PATH="$JAR_PATH"
 */
public class DeployContainer {

    public static void main(String[] args) throws Exception {
        KieServicesClient client = KsClient.build();
        installAndDeploy(client);
    }

    /**
     * Installs the KJAR into the KIE Server Maven repo and creates the
     * container. Idempotent: disposes an existing container before redeploying.
     */
    public static void installAndDeploy(KieServicesClient client) throws IOException, InterruptedException {
        installKjar();
        redeployContainer(client);
    }

    // ── Step 1: publish JAR into KIE Server embedded Maven repo via Maven ───

    static void installKjar() throws IOException, InterruptedException {
        String eap81 = KsClient.env("EAP81",
            System.getProperty("user.home") + "/BAMOE-8/BAMOE-8.1/jboss-eap-8.1");
        String repoUrl = "file://" + eap81 + "/repositories/kie/global";

        String jarPathOverride = KsClient.env("JAR_PATH", null);

        ProcessBuilder pb;

        if (jarPathOverride != null) {
            // Downloaded JAR: use install:install-file — no kjar/ source tree needed.
            pb = new ProcessBuilder(
                "mvn", "install:install-file",
                "-Dfile=" + jarPathOverride,
                "-DgroupId=" + KsClient.GROUP_ID,
                "-DartifactId=" + KsClient.ARTIFACT_ID,
                "-Dversion=" + KsClient.VERSION,
                "-Dpackaging=jar",
                "-DlocalRepositoryPath=" + eap81 + "/repositories/kie/global"
            );
            pb.directory(Paths.get(jarPathOverride).getParent().toFile());
        } else {
            // Normal build output: deploy to the KIE Server repo directly.
            String project = KsClient.env("PROJECT",
                Paths.get("").toAbsolutePath().getParent().toString());

            pb = new ProcessBuilder(
                "mvn", "deploy", "-DskipTests",
                "-DaltDeploymentRepository=kie-server::default::" + repoUrl
            );
            pb.directory(Paths.get(project, "kjar").toFile());
        }

        pb.inheritIO();
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Maven install step failed (exit " + exit + ")");
        }
        System.out.println("KJAR installed to: " + eap81 + "/repositories/kie/global");
    }

    // ── Step 2: dispose existing container (if any) and create fresh ────────

    static void redeployContainer(KieServicesClient client) {
        client.disposeContainer(KsClient.CONTAINER_ID);

        KieContainerResource container = new KieContainerResource(
            KsClient.CONTAINER_ID,
            new ReleaseId(KsClient.GROUP_ID, KsClient.ARTIFACT_ID, KsClient.VERSION));

        ServiceResponse<KieContainerResource> resp =
            client.createContainer(KsClient.CONTAINER_ID, container);

        if (resp.getType() != ServiceResponse.ResponseType.SUCCESS) {
            throw new IllegalStateException(
                "Failed to deploy container '" + KsClient.CONTAINER_ID + "': " + resp.getMsg());
        }
        System.out.println("Container deployed: " + KsClient.CONTAINER_ID + "  status=STARTED");
    }
}
