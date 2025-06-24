import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleHttpServer {
    private final Elevatorr elevatorA;
    private final Elevatorr elevatorB;
    private final List<Integer> globalPickupRequests;

    public SimpleHttpServer(Elevatorr elevatorA, Elevatorr elevatorB, List<Integer> globalPickupRequests) {
        this.elevatorA = elevatorA;
        this.elevatorB = elevatorB;
        this.globalPickupRequests = globalPickupRequests;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);

        // 🚀 Handle pickup requests
        server.createContext("/pickup", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            int floor = Integer.parseInt(params.getOrDefault("floor", "-1"));

            if (floor >= 0) {
                boolean aActive = !elevatorA.isInMaintenance();
                boolean bActive = !elevatorB.isInMaintenance();

                Elevatorr chosen = null;

                if (aActive && bActive) {
                    int distA = Math.abs(elevatorA.getCurrentFloor() - floor);
                    int distB = Math.abs(elevatorB.getCurrentFloor() - floor);
                    chosen = distA <= distB ? elevatorA : elevatorB;
                } else if (aActive) {
                    chosen = elevatorA;
                } else if (bActive) {
                    chosen = elevatorB;
                }

                if (chosen != null) {
                    chosen.addPickupRequest(floor);
                    System.out.println("Pickup assigned to Elevator " + (chosen == elevatorA ? "A" : "B"));
                    respond(exchange, "Pickup assigned.");
                } else {
                    globalPickupRequests.add(floor); // queue it globally
                    System.out.println("No active elevators. Request saved globally.");
                    respond(exchange, "All elevators in maintenance. Request queued.");
                }
            } else {
                respond(exchange, "Invalid floor.");
            }
        });

        // 🚀 Handle destination assignments
        server.createContext("/destination", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            int floor = Integer.parseInt(params.getOrDefault("floor", "-1"));
            String elevatorId = params.get("elevator");

            if ("A".equalsIgnoreCase(elevatorId) && !elevatorA.isInMaintenance()) {
                elevatorA.addDestinationRequest(floor);
                respond(exchange, "Assigned to Elevator A");
            } else if ("B".equalsIgnoreCase(elevatorId) && !elevatorB.isInMaintenance()) {
                elevatorB.addDestinationRequest(floor);
                respond(exchange, "Assigned to Elevator B");
            } else {
                respond(exchange, "Invalid or inactive elevator.");
            }
        });

        // 🚀 Maintenance Mode Toggle
        server.createContext("/maintenance", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            String elevatorId = params.get("elevator");
            String mode = params.get("mode"); // "on" or "off"

            if (elevatorId == null || mode == null) {
                respond(exchange, "Missing parameters.");
                return;
            }

            boolean on = mode.equalsIgnoreCase("on");

            if ("A".equalsIgnoreCase(elevatorId)) {
                elevatorA.setInMaintenance(on);
                respond(exchange, "Elevator A maintenance mode: " + mode.toUpperCase());
            } else if ("B".equalsIgnoreCase(elevatorId)) {
                elevatorB.setInMaintenance(on);
                respond(exchange, "Elevator B maintenance mode: " + mode.toUpperCase());
            } else {
                respond(exchange, "Invalid elevator ID.");
            }
        });

        // 🚀 Elevator Status
        server.createContext("/status", exchange -> {
            String responseJson = "{ " +
                    "\"elevatorA\": { \"floor\": " + elevatorA.getCurrentFloor() +
                    ", \"waiting\": " + elevatorA.isWaitingForDestination() +
                    ", \"inMaintenance\": " + elevatorA.isInMaintenance() + " }, " +
                    "\"elevatorB\": { \"floor\": " + elevatorB.getCurrentFloor() +
                    ", \"waiting\": " + elevatorB.isWaitingForDestination() +
                    ", \"inMaintenance\": " + elevatorB.isInMaintenance() + " } }";
            respond(exchange, responseJson);
        });

        // Timeout handler (optional)
        server.createContext("/timeout", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            int floor = Integer.parseInt(params.getOrDefault("floor", "-1"));
            System.out.println("Timeout received for floor: " + floor);
            respond(exchange, "Timeout recorded.");
        });

        server.setExecutor(null); // use default executor
        server.start();
        System.out.println("✅ Server started at http://localhost:8085/");
    }

    private void respond(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private Map<String, String> queryParams(String query) {
        if (query == null) return Collections.emptyMap();
        return Arrays.stream(query.split("&"))
                .map(kv -> kv.split("="))
                .filter(pair -> pair.length == 2)
                .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
    }
}
