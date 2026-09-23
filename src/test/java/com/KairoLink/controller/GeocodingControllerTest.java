package com.KairoLink.controller;

import com.KairoLink.dto.GeocodingResponse;
import com.KairoLink.service.NominatimGeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeocodingControllerTest {

    private NominatimGeocodingService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(NominatimGeocodingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GeocodingController(service)).build();
    }

    @Test
    void reverseEndpointReturnsLocation() throws Exception {
        when(service.reverse(new BigDecimal("28.6139"), new BigDecimal("77.209")))
                .thenReturn(new GeocodingResponse(
                        new BigDecimal("28.6139"), new BigDecimal("77.209"), "Main Road"));

        mockMvc.perform(get("/api/geocoding/reverse")
                        .param("latitude", "28.6139")
                        .param("longitude", "77.209"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Main Road"));

        verify(service).reverse(eq(new BigDecimal("28.6139")), eq(new BigDecimal("77.209")));
    }

    @Test
    void searchEndpointReturnsLocation() throws Exception {
        when(service.search("NIET")).thenReturn(new GeocodingResponse(
                new BigDecimal("28.4595"), new BigDecimal("77.5025"), "NIET"));

        mockMvc.perform(get("/api/geocoding/search").param("query", "NIET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.4595));

        verify(service).search("NIET");
    }
}
