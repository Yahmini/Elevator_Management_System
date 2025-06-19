import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

public class SimpleHttpServer {
    private final Elevatorr elevator;

    public SimpleHttpServer(Elevatorr elevator) {
        this.elevator = elevator;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8085), 0);

        // Handle /pickup?floor=2
        server.createContext("/pickup", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            int floor = Integer.parseInt(params.getOrDefault("floor", "-1"));
            elevator.addPickupRequest(floor);
            respond(exchange, "Pickup request received for floor: " + floor);
        });

        // Handle /destination?floor=5
        server.createContext("/destination", exchange -> {
            Map<String, String> params = queryParams(exchange.getRequestURI().getQuery());
            int floor = Integer.parseInt(params.getOrDefault("floor", "-1"));
            elevator.addDestinationRequest(floor);
            respond(exchange, "Destination request received for floor: " + floor);
        });

        // Handle /status
        server.createContext("/status", exchange -> {
            respond(exchange, "{ \"floor\": " + elevator.getCurrentFloor() + " }");
        });

        server.setExecutor(null); // use default
        server.start();
        System.out.println("HTTP Server started at http://localhost:8080/");
    }

    private void respond(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private Map<String, String> queryParams(String query) {
        return java.util.Arrays.stream(query.split("&"))
            .map(kv -> kv.split("="))
            .filter(pair -> pair.length == 2)
            .collect(java.util.stream.Collectors.toMap(pair -> pair[0], pair -> pair[1]));
    }
}
