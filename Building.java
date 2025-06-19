
import java.io.*;

public class Building {
    public static void main(String[] args) {
        Elevatorr elevator = new Elevatorr(10);

        Thread elevatorThread = new Thread(elevator);
        elevatorThread.start();

        try {
            new SimpleHttpServer(elevator).start(); // Start HTTP server
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
