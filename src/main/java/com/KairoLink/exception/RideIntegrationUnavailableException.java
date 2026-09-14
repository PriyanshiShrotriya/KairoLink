package com.KairoLink.exception;

public class RideIntegrationUnavailableException extends RuntimeException {

    public RideIntegrationUnavailableException() {
        super("Ride integration is not available until Phase 2 is merged");
    }
}
