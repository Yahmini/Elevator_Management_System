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

        server.createContext("/pickup", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            int floor = Integer.parseInt(params.getOrDefault("floor", "-1"));

            if (floor >= 0) {
                Elevatorr chosen = chooseElevator(floor);
                if (chosen != null) {
                    chosen.addPickupRequest(floor);
                    System.out.println("Pickup assigned to Elevator " + (chosen == elevatorA ? "A" : "B"));
                    respond(exchange, "Pickup assigned.");
                } else {
                    globalPickupRequests.add(floor);
                    respond(exchange, "No available elevators. Request queued.");
                }
            } else {
                respond(exchange, "Invalid floor.");
            }
        });

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

        server.createContext("/maintenance", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            String elevatorId = params.get("elevator");
            String mode = params.get("mode");

            if (elevatorId == null || mode == null) {
                respond(exchange, "Missing parameters.");
                return;
            }

            boolean on = mode.equalsIgnoreCase("on");

            if ("A".equalsIgnoreCase(elevatorId)) {
                elevatorA.setInMaintenance(on);
                respond(exchange, "Elevator A maintenance: " + mode.toUpperCase());
            } else if ("B".equalsIgnoreCase(elevatorId)) {
                elevatorB.setInMaintenance(on);
                respond(exchange, "Elevator B maintenance: " + mode.toUpperCase());
            } else {
                respond(exchange, "Invalid elevator ID.");
            }
        });

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

        server.setExecutor(null);
        server.start();
        System.out.println("Server started at http://localhost:8085/");
    }
    private Elevatorr chooseElevator(int floor) {
    boolean aActive = !elevatorA.isInMaintenance();
    boolean bActive = !elevatorB.isInMaintenance();

    if (!aActive && !bActive) return null;
    if (aActive && !bActive) return elevatorA;
    if (!aActive && bActive) return elevatorB;

    int aTasks = elevatorA.getTotalRequests();
    int bTasks = elevatorB.getTotalRequests();
    int aDist = Math.abs(elevatorA.getCurrentFloor() - floor);
    int bDist = Math.abs(elevatorB.getCurrentFloor() - floor);

    boolean aOnWay = elevatorA.isMovingToward(floor);
    boolean bOnWay = elevatorB.isMovingToward(floor);

    if (aOnWay && !bOnWay) return elevatorA;
    if (!aOnWay && bOnWay) return elevatorB;


    if (aTasks < bTasks) return elevatorA;
    if (bTasks < aTasks) return elevatorB;

    return aDist <= bDist ? elevatorA : elevatorB;
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
