package com.KairoLink.service;

import com.KairoLink.dto.RideSearchRequest;
import com.KairoLink.exception.RideIntegrationUnavailableException;
import com.KairoLink.ride.RideGateway;
import com.KairoLink.ride.RideSummary;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RideSearchService {

    private final ObjectProvider<RideGateway> rideGatewayProvider;

    public RideSearchService(ObjectProvider<RideGateway> rideGatewayProvider) {
        this.rideGatewayProvider = rideGatewayProvider;
    }

    public List<RideSummary> search(RideSearchRequest request) {
        RideGateway gateway = requireGateway();
        return gateway.searchRides(
                request.getSource().trim(),
                request.getDestination().trim(),
                request.getDate());
    }

    public RideSummary findBookableRide(Long rideId) {
        return requireGateway().findBookableRide(rideId);
    }

    private RideGateway requireGateway() {
        RideGateway gateway = rideGatewayProvider.getIfAvailable();
        if (gateway == null) {
            throw new RideIntegrationUnavailableException();
        }
        return gateway;
    }
}
