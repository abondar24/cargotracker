package org.eclipse.cargotracker.application;

import io.restassured.http.ContentType;
import org.eclipse.cargotracker.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeploymentTest extends BaseIntegrationTest {


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
