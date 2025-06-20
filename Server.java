
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server implements Runnable {
    private Elevatorr elevator;

    public Server(Elevatorr elevator) {
        this.elevator = elevator;
    }

    public void run() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                Integer enteredFloor = elevator.pollEnteredFloor();
                if (enteredFloor != null) {
                    System.out.print("Person entered at floor " + enteredFloor + ". Enter destination (within 10 sec): ");

                    final String[] destinationInput = {null};

                    Thread inputThread = new Thread(() -> {
                        try {
                            destinationInput[0] = scanner.nextLine().trim();
                        } catch (Exception ignored) {
                        }
                    });

                    inputThread.start();

                    try {
                        inputThread.join(10000); // Wait max 10 seconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (destinationInput[0] != null) {
                        try {
                            int destination = Integer.parseInt(destinationInput[0]);
                            elevator.addDestinationRequest(destination);
                            System.out.println("Destination " + destination + " added to queue.");
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid destination input.");
                            elevator.resumeAfterTimeout(); // Resume elevator if input invalid
                        }
                    } else {
                        System.out.println("Timeout: No destination entered. Elevator resuming...");
                        inputThread.interrupt(); // Safely interrupt thread blocked on input
                        elevator.resumeAfterTimeout(); // Resume elevator
                    }
                }

                try {
                    Thread.sleep(500); // polling interval
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Server socket to receive pickup requests
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
