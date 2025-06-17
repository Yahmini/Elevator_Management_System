// Server.java
import java.io.*;
import java.net.*;

public class Server implements Runnable {
    private Elevator elevator;

    public Server(Elevator elevator) {
        this.elevator = elevator;
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server ready. Waiting for floor requests...");
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    try {
                        int requestedFloor = Integer.parseInt(inputLine);
                        System.out.println("Received request for floor: " + requestedFloor);
                        elevator.addRequest(requestedFloor);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid floor input: " + inputLine);
                    }
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

