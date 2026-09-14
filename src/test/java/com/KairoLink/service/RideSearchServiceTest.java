package com.KairoLink.service;

import com.KairoLink.dto.RideSearchRequest;
import com.KairoLink.ride.RideGateway;
import com.KairoLink.ride.RideSummary;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RideSearchServiceTest {

    @Mock
    private ObjectProvider<RideGateway> rideGatewayProvider;

    @Mock
    private RideGateway rideGateway;

    @Test
    void forwardsNormalizedSearchToRideGateway() {
        when(rideGatewayProvider.getIfAvailable()).thenReturn(rideGateway);
        RideSearchRequest request = new RideSearchRequest();
        request.setSource("  Delhi ");
        request.setDestination(" Noida ");
        request.setDate(LocalDate.of(2026, 9, 20));
        when(rideGateway.searchRides("Delhi", "Noida", request.getDate()))
                .thenReturn(List.of());

        assertEquals(List.of(), new RideSearchService(rideGatewayProvider).search(request));

        verify(rideGateway).searchRides("Delhi", "Noida", request.getDate());
    }
}
