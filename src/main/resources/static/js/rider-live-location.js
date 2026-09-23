(function () {
    "use strict";

    var sessions = document.querySelectorAll("[data-rider-live-location-session]");
    if (!sessions.length || typeof maplibregl === "undefined") {
        return;
    }

    sessions.forEach(function (session) {
        var rideId = session.dataset.rideId;
        var mapElement = session.querySelector("[data-rider-location-map]");
        var status = session.querySelector("[data-rider-location-status]");
        var marker = null;
        var pollTimer = null;
        var routeSource = "rider-ride-route";

        function coordinateValue(value) {
            return value === undefined || value.trim() === "" ? null : Number(value);
        }

        var sourceLatitude = coordinateValue(session.dataset.sourceLatitude);
        var sourceLongitude = coordinateValue(session.dataset.sourceLongitude);
        var destinationLatitude = coordinateValue(session.dataset.destinationLatitude);
        var destinationLongitude = coordinateValue(session.dataset.destinationLongitude);

        var map = new maplibregl.Map({
            container: mapElement,
            center: [sourceLongitude, sourceLatitude],
            zoom: 12,
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

        function drawRoute(coordinates) {
            var data = {
                type: "Feature",
                geometry: {type: "LineString", coordinates: coordinates}
            };
            if (map.getSource(routeSource)) {
                map.getSource(routeSource).setData(data);
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
                    setStatus("Route loaded. Waiting for the driver's location...");
                })
                .catch(function () {
                    setStatus("Route preview is unavailable. Driver location may still appear.");
                });
        }

        function showLocation(location) {
            var coordinates = [location.longitude, location.latitude];
            if (!marker) {
                marker = new maplibregl.Marker({color: "#2e8b74"})
                    .setLngLat(coordinates)
                    .addTo(map);
            } else {
                marker.setLngLat(coordinates);
            }
            setStatus("Driver location updated.");
        }

        function pollLocation() {
            fetch("/api/rides/" + encodeURIComponent(rideId) + "/driver-location")
                .then(function (response) {
                    if (response.status === 404) {
                        setStatus("Driver location is not available yet.");
                        return null;
                    }
                    if (!response.ok) {
                        throw new Error("Location request failed");
                    }
                    return response.json();
                })
                .then(function (location) {
                    if (location) {
                        showLocation(location);
                    }
                })
                .catch(function () {
                    setStatus("Driver location is temporarily unavailable.");
                });
        }

        function stopPolling() {
            if (pollTimer !== null) {
                window.clearInterval(pollTimer);
                pollTimer = null;
            }
        }

        map.on("load", function () {
            loadRoute();
            pollLocation();
            pollTimer = window.setInterval(pollLocation, 10000);
        });
        window.addEventListener("pagehide", stopPolling);
    });
})();
