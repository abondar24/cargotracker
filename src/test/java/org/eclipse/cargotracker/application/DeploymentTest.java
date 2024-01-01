package org.eclipse.cargotracker.application;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class DeploymentTest {
    static MountableFile warFile = MountableFile
            .forHostPath(Paths.get("target/cargo-tracker.war").toAbsolutePath(), 511);

    @Container
    static GenericContainer  cargoContainer= new GenericContainer("payara/server-full:6.2023.12-jdk17")
            .withCopyFileToContainer(warFile,"/opt/payara/deployments/cargo-tracker.war")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/cargo-tracker"));

    @Test
    void isDeployed(){
        assertTrue(cargoContainer.isRunning());
    }

    @Test
    void healthcheckTest(){
        List<LinkedHashMap<String,Object>> res = given().get(String.format("http://localhost:%d/cargo-tracker/rest/cargo",
                cargoContainer.getMappedPort(8080)))
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .contentType(ContentType.JSON)
                .extract()
                .as(List.class);

        assertEquals(4,res.size());

        res.forEach(r-> assertEquals(r.size(),7));
    }

    @AfterAll
    static void shutdown(){
        cargoContainer.stop();
    }
}
