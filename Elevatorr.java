import java.util.*;
import java.util.stream.Stream;


public class Elevatorr implements Runnable {
    private int currentFloor = 0;
    private boolean goingUp = true;
    private boolean pause = false;

    private final Set<Integer> pickupRequests = new TreeSet<>();
    private final Set<Integer> destinationRequests = new TreeSet<>();
    private final Queue<Integer> enteredFloors = new LinkedList<>();

    // Add pickup request
    public synchronized void addPickupRequest(int floor) {
        pickupRequests.add(floor);
    }

    // Add destination after person enters
    public synchronized void addDestinationRequest(int floor) {
        destinationRequests.add(floor);
        pause = false;
        notifyAll(); // resume elevator
    }

    // Used by server to detect that a person has entered
    public synchronized Integer pollEnteredFloor() {
        return enteredFloors.poll();
    }

    // Elevator thread
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    while (pause) {
                        wait(); // Pause until destination entered
                    }
                }
                Thread.sleep(1000); // Simulate time between floors
                processRequests();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Core elevator movement logic
    private synchronized void processRequests() {
        System.out.println("Elevator at floor: " + currentFloor);

        // Person is entering elevator at pickup floor
        if (pickupRequests.contains(currentFloor)) {
            System.out.println("✅ Person picked up at floor: " + currentFloor);
            pickupRequests.remove(currentFloor);
            enteredFloors.add(currentFloor);
            pause = true;
            //return;
        }

        // Person is being dropped off
        if (destinationRequests.contains(currentFloor)) {
            System.out.println("🏁 Person dropped off at floor: " + currentFloor);
            destinationRequests.remove(currentFloor);
        }

        // Determine next direction intelligently
        Integer nextUp = findNextAbove();
        Integer nextDown = findNextBelow();

        if (goingUp) {
            if (nextUp != null) {
                currentFloor++;
            } else if (nextDown != null) {
                goingUp = false;
                currentFloor--;
            }
        } else {
            if (nextDown != null) {
                currentFloor--;
            } else if (nextUp != null) {
                goingUp = true;
                currentFloor++;
            }
        }
    }

    // Get next request above current floor
    private Integer findNextAbove() {
        return Stream.concat(pickupRequests.stream(), destinationRequests.stream())
                .filter(f -> f > currentFloor)
                .min(Integer::compareTo)
                .orElse(null);
    }

    // Get next request below current floor
    private Integer findNextBelow() {
        return Stream.concat(pickupRequests.stream(), destinationRequests.stream())
                .filter(f -> f < currentFloor)
                .max(Integer::compareTo)
                .orElse(null);
    }
}
