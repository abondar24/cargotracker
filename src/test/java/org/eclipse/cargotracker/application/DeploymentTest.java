package org.eclipse.cargotracker.application;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import java.util.LinkedHashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("integration")
public class DeploymentTest {

    private final Logger LOGGER = LoggerFactory.getLogger(DeploymentTest.class);


    private final MountableFile warFile = MountableFile.forHostPath(
            Paths.get("target/cargo-tracker.war").toAbsolutePath(), 511);


    @Container
    private  GenericContainer<?> cargoContainer = new GenericContainer<>("payara/server-full")
            .withCopyFileToContainer(warFile, "/opt/payara/deployments/cargo-tracker.war")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/cargo-tracker"))
            .withStartupTimeout(Duration.ofSeconds(200))
            .withLogConsumer(new Slf4jLogConsumer(LOGGER));



    @Test
    void isDeployed() {
        assertTrue(cargoContainer.isRunning());

        List<LinkedHashMap<String, Object>> res = given().get(String.format("http://localhost:%d/cargo-tracker/rest/cargo",
                        cargoContainer.getMappedPort(8080)))
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .contentType(ContentType.JSON)
                .extract()
                .as(List.class);

        assertEquals(4, res.size());

        res.forEach(r -> assertEquals(r.size(), 7));
        cargoContainer.stop();
    }

}
