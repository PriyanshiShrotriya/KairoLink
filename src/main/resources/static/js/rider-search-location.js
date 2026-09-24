(function () {
    var sourceInput = document.getElementById("source");
    var locationButton = document.querySelector("[data-use-current-location]");
    var status = document.querySelector("[data-current-location-status]");

    if (!sourceInput || !locationButton || !status) {
        return;
    }

    var requestVersion = 0;

    function setStatus(message) {
        status.textContent = message;
    }

    function setBusy(isBusy) {
        locationButton.disabled = isBusy;
        locationButton.setAttribute("aria-busy", isBusy ? "true" : "false");
    }

    sourceInput.addEventListener("input", function () {
        requestVersion++;
        setBusy(false);
    });

    locationButton.addEventListener("click", function () {
        var currentRequest = ++requestVersion;

        if (!navigator.geolocation) {
            setStatus("Current location is not supported. Enter your starting point manually.");
            return;
        }

        setBusy(true);
        setStatus("Finding your current location...");

        navigator.geolocation.getCurrentPosition(function (position) {
            if (currentRequest !== requestVersion) {
                return;
            }

            setStatus("Location found. Finding the address...");
            fetch("/api/geocoding/reverse?latitude="
                + encodeURIComponent(position.coords.latitude)
                + "&longitude="
                + encodeURIComponent(position.coords.longitude))
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Reverse geocoding failed");
                    }
                    return response.json();
                })
                .then(function (location) {
                    if (currentRequest !== requestVersion) {
                        return;
                    }
                    if (!location.displayName) {
                        throw new Error("Address was not found");
                    }
                    sourceInput.value = location.displayName;
                    setStatus("Current location used as your starting point. You can edit it if needed.");
                })
                .catch(function () {
                    if (currentRequest === requestVersion) {
                        setStatus("Could not find an address for your location. Enter your starting point manually.");
                    }
                })
                .finally(function () {
                    if (currentRequest === requestVersion) {
                        setBusy(false);
                    }
                });
        }, function (error) {
            if (currentRequest !== requestVersion) {
                return;
            }

            if (error.code === error.PERMISSION_DENIED) {
                setStatus("Location permission was denied. Enter your starting point manually.");
            } else if (error.code === error.TIMEOUT) {
                setStatus("Location request timed out. Enter your starting point manually.");
            } else {
                setStatus("Current location is unavailable. Enter your starting point manually.");
            }
            setBusy(false);
        }, {
            enableHighAccuracy: true,
            maximumAge: 10000,
            timeout: 15000
        });
    });
})();
