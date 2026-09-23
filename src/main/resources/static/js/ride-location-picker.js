(function () {
    "use strict";

    var picker = document.querySelector("[data-ride-location-picker]");
    if (!picker || typeof maplibregl === "undefined") {
        return;
    }

    var mapElement = picker.querySelector("[data-location-map]");
    var status = picker.querySelector("[data-location-status]");
    var routeSummary = picker.querySelector("[data-location-route]");
    var targetButtons = picker.querySelectorAll("[data-location-target]");
    var fields = {
        source: {
            latitude: document.getElementById("sourceLatitude"),
            longitude: document.getElementById("sourceLongitude"),
            color: "#2e8b74"
        },
        destination: {
            latitude: document.getElementById("destinationLatitude"),
            longitude: document.getElementById("destinationLongitude"),
            color: "#e15554"
        }
    };
    var targets = {
        source: "starting point",
        destination: "destination"
    };
    var markers = {};
    var activeTarget = null;
    var routeRequest = 0;
    var routeSource = "route";

    function numberFromField(field) {
        if (!field || field.value.trim() === "") {
            return null;
        }
        var value = Number(field.value);
        return Number.isFinite(value) ? value : null;
    }

    function coordinatesFor(target) {
        var field = fields[target];
        var latitude = numberFromField(field.latitude);
        var longitude = numberFromField(field.longitude);
        return latitude === null || longitude === null ? null : [longitude, latitude];
    }

    var sourceCoordinates = coordinatesFor("source");
    var destinationCoordinates = coordinatesFor("destination");
    var initialCoordinates = sourceCoordinates || destinationCoordinates || [77.5025, 28.4595];

    var map = new maplibregl.Map({
        container: mapElement,
        center: initialCoordinates,
        zoom: sourceCoordinates || destinationCoordinates ? 12 : 10,
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

    function updateStatus(message) {
        status.textContent = message;
    }

    function selectTarget(target) {
        activeTarget = target;
        targetButtons.forEach(function (button) {
            button.classList.toggle("is-active", button.dataset.locationTarget === target);
        });
        updateStatus("Click the map to set the " + targets[target] + ".");
    }

    function setMarker(target, coordinates) {
        if (markers[target]) {
            markers[target].setLngLat(coordinates);
            return;
        }
        markers[target] = new maplibregl.Marker({color: fields[target].color})
            .setLngLat(coordinates)
            .addTo(map);
    }

    function setLocation(target, coordinates) {
        var field = fields[target];
        field.latitude.value = coordinates[1].toFixed(6);
        field.longitude.value = coordinates[0].toFixed(6);
        setMarker(target, coordinates);
        map.flyTo({center: coordinates, essential: true});
        updateStatus(targets[target].charAt(0).toUpperCase() + targets[target].slice(1)
            + " set. Select another point or location.");
        requestRoute();
    }

    function formatDuration(seconds) {
        var minutes = Math.round(seconds / 60);
        if (minutes < 60) {
            return minutes + " min";
        }
        return Math.floor(minutes / 60) + " hr " + (minutes % 60) + " min";
    }

    function formatDistance(meters) {
        return meters >= 1000
            ? (meters / 1000).toFixed(1) + " km"
            : Math.round(meters) + " m";
    }

    function requestRoute() {
        var source = coordinatesFor("source");
        var destination = coordinatesFor("destination");
        if (!source || !destination) {
            routeSummary.textContent = "";
            removeRoute();
            return;
        }
        var requestId = ++routeRequest;
        routeSummary.textContent = "Calculating route...";
        fetch("/api/routes?sourceLatitude=" + encodeURIComponent(source[1])
            + "&sourceLongitude=" + encodeURIComponent(source[0])
            + "&destinationLatitude=" + encodeURIComponent(destination[1])
            + "&destinationLongitude=" + encodeURIComponent(destination[0]))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Route request failed");
                }
                return response.json();
            })
            .then(function (route) {
                if (requestId !== routeRequest) {
                    return;
                }
                if (!Array.isArray(route.geometry) || route.geometry.length < 2) {
                    throw new Error("Route geometry was invalid");
                }
                drawRoute(route.geometry);
                routeSummary.textContent = formatDistance(route.distanceMeters)
                    + " · approximately " + formatDuration(route.durationSeconds);
            })
            .catch(function () {
                if (requestId !== routeRequest) {
                    return;
                }
                removeRoute();
                routeSummary.textContent = "Route preview is unavailable. You can still save the ride.";
            });
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

    function removeRoute() {
        if (map.getLayer(routeSource)) {
            map.removeLayer(routeSource);
        }
        if (map.getSource(routeSource)) {
            map.removeSource(routeSource);
        }
    }

    targetButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            selectTarget(button.dataset.locationTarget);
        });
    });

    map.on("click", function (event) {
        if (!activeTarget) {
            updateStatus("Choose starting point or destination before clicking the map.");
            return;
        }
        setLocation(activeTarget, [event.lngLat.lng, event.lngLat.lat]);
        activeTarget = null;
        targetButtons.forEach(function (button) {
            button.classList.remove("is-active");
        });
    });

    map.on("load", function () {
        if (sourceCoordinates) {
            setMarker("source", sourceCoordinates);
        }
        if (destinationCoordinates) {
            setMarker("destination", destinationCoordinates);
        }
        requestRoute();
    });
})();
