//Building.java
import java.util.*;

public class Building {
    public static void main(String[] args) {
        List<Integer> globalPickupRequests = Collections.synchronizedList(new ArrayList<>());

        Elevatorr elevatorA = new Elevatorr("A", 100, globalPickupRequests);
        Elevatorr elevatorB = new Elevatorr("B", 100, globalPickupRequests);

        Thread threadA = new Thread(elevatorA);
        Thread threadB = new Thread(elevatorB);

        threadA.start();
        threadB.start();

        try {
            new SimpleHttpServer(elevatorA, elevatorB, globalPickupRequests).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
