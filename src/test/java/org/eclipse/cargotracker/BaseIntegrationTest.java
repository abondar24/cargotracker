package org.eclipse.cargotracker;

import org.eclipse.cargotracker.application.DeploymentTest;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.time.Duration;

@Testcontainers
@Tag("integration")
public class BaseIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentTest.class);


    private static final MountableFile warFile = MountableFile.forHostPath(
            Paths.get("target/cargo-tracker.war").toAbsolutePath(), 511);


    @Container
    protected static GenericContainer<?> cargoContainer = new GenericContainer<>("payara/server-full")
            .withCopyFileToContainer(warFile, "/opt/payara/deployments/cargo-tracker.war")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/cargo-tracker"))
            .withStartupTimeout(Duration.ofSeconds(500))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER));


}
