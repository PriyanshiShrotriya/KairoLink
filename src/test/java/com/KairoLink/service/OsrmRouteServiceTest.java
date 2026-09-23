package com.KairoLink.service;

import com.KairoLink.dto.RouteResponse;
import com.KairoLink.exception.RouteCalculationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class OsrmRouteServiceTest {

    @Test
    void parsesOsrmRouteResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://osrm.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://osrm.test/route/v1/driving/77.2090%2C28.6139%3B77.3910%2C28.5355"
                        + "?overview=full&geometries=geojson"))
                .andRespond(withSuccess("""
                        {
                          "code": "Ok",
                          "routes": [{
                            "distance": 12345.6,
                            "duration": 2345.0,
                            "geometry": {
                              "coordinates": [[77.209, 28.6139], [77.391, 28.5355]]
                            }
                          }]
                        }
                        """, APPLICATION_JSON));

        OsrmRouteService service = new OsrmRouteService(
                builder, "http://osrm.test");
        RouteResponse response = service.calculate(
                new BigDecimal("28.6139"), new BigDecimal("77.2090"),
                new BigDecimal("28.5355"), new BigDecimal("77.3910"));

        assertEquals(new BigDecimal("12345.6"), response.distanceMeters());
        assertEquals(new BigDecimal("2345.0"), response.durationSeconds());
        assertEquals(2, response.geometry().size());
        server.verify();
    }

    @Test
    void rejectsInvalidCoordinatesBeforeCallingOsrm() {
        OsrmRouteService service = new OsrmRouteService(
                RestClient.builder(), "http://osrm.test");

        assertThrows(RouteCalculationException.class, () -> service.calculate(
                new BigDecimal("91"), new BigDecimal("77"),
                new BigDecimal("28"), new BigDecimal("77")));
    }
}
