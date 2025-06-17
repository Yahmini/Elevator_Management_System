// Server.java
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server implements Runnable {
    private Elevatorr elevator;

    public Server(Elevatorr elevator) {
        this.elevator = elevator;
    }

    public void run() {
        // Thread to collect destination input when someone enters
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                Integer enteredFloor = elevator.pollEnteredFloor();
                if (enteredFloor != null) {
                    System.out.print("Person entered at floor " + enteredFloor + ". Enter destination: ");
                    try {
                        int destination = Integer.parseInt(scanner.nextLine().trim());
                        elevator.addDestinationRequest(destination);
                        System.out.println("Destination " + destination + " added to queue.");
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid destination input.");
                    }
                }

                try {
                    Thread.sleep(500); // Polling interval
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Main server loop to receive pickup requests
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server ready. Waiting for pickup requests...");
            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    try {
                        int requestedFloor = Integer.parseInt(inputLine);
                        System.out.println("Received pickup request at floor: " + requestedFloor);
                        elevator.addPickupRequest(requestedFloor);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid pickup input: " + inputLine);
                    }
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
