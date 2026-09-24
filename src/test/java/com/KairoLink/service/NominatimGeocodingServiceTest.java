package com.KairoLink.service;

import com.KairoLink.dto.GeocodingResponse;
import com.KairoLink.exception.GeocodingException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimGeocodingServiceTest {

    @Test
    void reverseGeocodingReturnsConciseAddressAndCoordinates() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://nominatim.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://nominatim.test/reverse?format=jsonv2&lat=28.6139&lon=77.209"))
                .andExpect(header("User-Agent", "KairoLinkTest/1.0"))
                .andRespond(withSuccess("""
                        {
                          "lat": "28.6139",
                          "lon": "77.2090",
                          "display_name": "Fallback address",
                          "address": {
                            "road": "Main Road",
                            "city": "New Delhi",
                            "state": "Delhi"
                          }
                        }
                        """, APPLICATION_JSON));

        NominatimGeocodingService service = new NominatimGeocodingService(
                builder, "http://nominatim.test", "KairoLinkTest/1.0");
        GeocodingResponse response = service.reverse(
                new BigDecimal("28.6139"), new BigDecimal("77.209"));

        assertEquals(new BigDecimal("28.6139"), response.latitude());
        assertEquals(new BigDecimal("77.2090"), response.longitude());
        assertEquals("Main Road, New Delhi, Delhi", response.displayName());
        server.verify();
    }

    @Test
    void searchGeocodingReturnsFirstResult() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://nominatim.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://nominatim.test/search?format=jsonv2&limit=1&q=NIET"))
                .andRespond(withSuccess("""
                        [{"lat":"28.4595","lon":"77.5025","display_name":"NIET"}]
                        """, APPLICATION_JSON));

        GeocodingResponse response = new NominatimGeocodingService(
                builder, "http://nominatim.test", "KairoLinkTest/1.0").search("NIET");

        assertEquals(new BigDecimal("28.4595"), response.latitude());
        assertEquals(new BigDecimal("77.5025"), response.longitude());
        assertEquals("NIET", response.displayName());
        server.verify();
    }

    @Test
    void rejectsInvalidReverseCoordinates() {
        NominatimGeocodingService service = new NominatimGeocodingService(
                RestClient.builder(), "http://nominatim.test", "KairoLinkTest/1.0");

        assertThrows(GeocodingException.class,
                () -> service.reverse(new BigDecimal("91"), BigDecimal.ZERO));
    }
}
