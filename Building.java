public class Building {
    public static Elevator elevator = new Elevator();

    public static void main(String[] args) {
        Thread elevatorThread = new Thread(elevator);
        elevatorThread.start();

        Server server = new Server(elevator);
        Thread serverThread = new Thread(server);
        serverThread.start();
    }
}
