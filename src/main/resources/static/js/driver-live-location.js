(function () {
    "use strict";

    var sessions = document.querySelectorAll("[data-driver-location-session]");
    if (!sessions.length || typeof maplibregl === "undefined") {
        return;
    }

    sessions.forEach(function (session) {
        var rideId = session.dataset.rideId;
        var mapElement = session.querySelector("[data-driver-location-map]");
        var status = session.querySelector("[data-driver-location-status]");
        var startButton = session.querySelector("[data-location-start]");
        var stopButton = session.querySelector("[data-location-stop]");
        var csrf = session.querySelector("[data-location-csrf]").value;
        var watchId = null;
        var updateTimer = null;
        var latestPosition = null;
        var marker = null;
        var routeSource = "ride-route";
        function coordinateValue(value) {
            return value === undefined || value.trim() === "" ? null : Number(value);
        }
        var sourceLatitude = coordinateValue(session.dataset.sourceLatitude);
        var sourceLongitude = coordinateValue(session.dataset.sourceLongitude);
        var destinationLatitude = coordinateValue(session.dataset.destinationLatitude);
        var destinationLongitude = coordinateValue(session.dataset.destinationLongitude);
        var map = new maplibregl.Map({
            container: mapElement,
            center: [77.5025, 28.4595],
            zoom: 10,
            style: {
                version: 8,
                sources: {
                    "osm-tiles": {
                        type: "raster",
                        tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
                        tileSize: 256,
                        attribution: "© OpenStreetMap contributors"
                    }
                },
                layers: [{
                    id: "osm-tiles",
                    type: "raster",
                    source: "osm-tiles"
                }]
            }
        });
        map.addControl(new maplibregl.NavigationControl(), "top-right");

        function setStatus(message) {
            status.textContent = message;
        }

        function hasRouteCoordinates() {
            return Number.isFinite(sourceLatitude) && Number.isFinite(sourceLongitude)
                && Number.isFinite(destinationLatitude) && Number.isFinite(destinationLongitude);
        }

        function drawRoute(coordinates) {
            var source = map.getSource(routeSource);
            var data = {
                type: "Feature",
                geometry: {type: "LineString", coordinates: coordinates}
            };
            if (source) {
                source.setData(data);
                return;
            }
            map.addSource(routeSource, {type: "geojson", data: data});
            map.addLayer({
                id: routeSource,
                type: "line",
                source: routeSource,
                paint: {"line-color": "#1b2a4a", "line-width": 4, "line-opacity": 0.8}
            });
        }

        function loadRoute() {
            if (!hasRouteCoordinates()) {
                setStatus("Route coordinates are unavailable. Location sharing is off.");
                return;
            }
            fetch("/api/routes?sourceLatitude=" + encodeURIComponent(sourceLatitude)
                + "&sourceLongitude=" + encodeURIComponent(sourceLongitude)
                + "&destinationLatitude=" + encodeURIComponent(destinationLatitude)
                + "&destinationLongitude=" + encodeURIComponent(destinationLongitude))
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("Route request failed");
                    }
                    return response.json();
                })
                .then(function (route) {
                    if (!Array.isArray(route.geometry) || route.geometry.length < 2) {
                        throw new Error("Route geometry was invalid");
                    }
                    drawRoute(route.geometry);
                })
                .catch(function () {
                    setStatus("Route preview is unavailable. Location sharing is still available.");
                });
        }

        function showPosition(position) {
            var coordinates = [
                position.coords.longitude,
                position.coords.latitude
            ];
            if (!marker) {
                marker = new maplibregl.Marker({color: "#2e8b74"})
                    .setLngLat(coordinates)
                    .addTo(map);
            } else {
                marker.setLngLat(coordinates);
            }
            map.flyTo({center: coordinates, zoom: 14, essential: true});
        }

        function sendPosition() {
            if (!latestPosition) {
                return;
            }
            var coordinates = {
                latitude: latestPosition.coords.latitude,
                longitude: latestPosition.coords.longitude
            };
            fetch("/api/rides/" + encodeURIComponent(rideId) + "/driver-location", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-TOKEN": csrf
                },
                body: JSON.stringify(coordinates)
            }).then(function (response) {
                if (!response.ok) {
                    throw new Error("Location update failed");
                }
                setStatus("Location sharing is active.");
            }).catch(function () {
                setStatus("Location update failed. You can try again.");
            });
        }

        function loadCurrentPosition() {
            return fetch("/api/rides/" + encodeURIComponent(rideId) + "/driver-location")
                .then(function (response) {
                    if (!response.ok) {
                        return null;
                    }
                    return response.json();
                })
                .then(function (location) {
                    if (!location) {
                        return;
                    }
                    showPosition({
                        coords: {
                            latitude: location.latitude,
                            longitude: location.longitude
                        }
                    });
                })
                .catch(function () {
                    setStatus("No previous location is available yet.");
                });
        }

        function stopSharing() {
            if (watchId !== null) {
                navigator.geolocation.clearWatch(watchId);
                watchId = null;
            }
            if (updateTimer !== null) {
                window.clearInterval(updateTimer);
                updateTimer = null;
            }
            latestPosition = null;
            startButton.hidden = false;
            stopButton.hidden = true;
            setStatus("Location sharing is off.");
        }

        function startSharing() {
            if (!navigator.geolocation) {
                setStatus("Location is not available in this browser.");
                return;
            }
            startButton.hidden = true;
            stopButton.hidden = false;
            setStatus("Requesting location permission...");
            loadCurrentPosition();
            navigator.geolocation.getCurrentPosition(function (position) {
                latestPosition = position;
                showPosition(position);
                sendPosition();
                updateTimer = window.setInterval(sendPosition, 10000);
                watchId = navigator.geolocation.watchPosition(function (nextPosition) {
                    latestPosition = nextPosition;
                    showPosition(nextPosition);
                }, function () {
                    setStatus("Location became unavailable. Sharing has stopped.");
                    stopSharing();
                }, {enableHighAccuracy: true, maximumAge: 10000, timeout: 15000});
            }, function (error) {
                stopSharing();
                if (error.code === error.PERMISSION_DENIED) {
                    setStatus("Location permission was denied. Enable it to share your position.");
                } else {
                    setStatus("Your current location is unavailable. Please try again.");
                }
            }, {enableHighAccuracy: true, maximumAge: 10000, timeout: 15000});
        }

        startButton.addEventListener("click", startSharing);
        stopButton.addEventListener("click", stopSharing);
        window.addEventListener("pagehide", stopSharing);
        map.on("load", loadRoute);
    });
})();
