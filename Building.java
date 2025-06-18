import java.util.Scanner;

public class Building {
    public static Elevatorr elevator;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter number of floors in the building: ");
        int maxFloor = scanner.nextInt();

        elevator = new Elevatorr(maxFloor);

        Thread elevatorThread = new Thread(elevator);
        elevatorThread.start();

        Server server = new Server(elevator);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }
}

