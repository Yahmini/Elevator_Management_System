import java.util.*;
import java.util.stream.Stream;

public class Elevatorr implements Runnable {
    private int currentFloor = 0;
    private boolean goingUp = true;
    private boolean pause = false;
    private final int maxFloor;

    private final Set<Integer> pickupRequests = new TreeSet<>();
    private final Set<Integer> destinationRequests = new TreeSet<>();
    private final Queue<Integer> enteredFloors = new LinkedList<>();

    public Elevatorr(int maxFloor) {
        this.maxFloor = maxFloor;
    }

    public synchronized void addPickupRequest(int floor) {
        if (floor >= 0 && floor <= maxFloor) {
            pickupRequests.add(floor);
        } else {
            System.out.println("Invalid floor: " + floor);
        }
    }

    public synchronized void addDestinationRequest(int floor) {
        if (floor >= 0 && floor <= maxFloor) {
            destinationRequests.add(floor);
            pause = false;
            notifyAll(); // Wake elevator
        } else {
            System.out.println("Invalid destination: " + floor);
        }
    }

    public synchronized Integer pollEnteredFloor() {
        return enteredFloors.poll();
    }

    public synchronized void resumeAfterTimeout() {
        pause = false;
        notifyAll();
    }

    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    while (pause) {
                        wait(); // Wait for destination input or timeout
                    }
                }
                Thread.sleep(1500);
                processRequests();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void processRequests() {
        System.out.println("Elevator at floor: " + currentFloor);

        if (pickupRequests.contains(currentFloor)) {
            System.out.println("Person picked up at floor: " + currentFloor);
            pickupRequests.remove(currentFloor);
            enteredFloors.add(currentFloor);
            pause = true;
        }
        
        if (destinationRequests.contains(currentFloor)) {
            System.out.println("Person dropped off at floor: " + currentFloor);
            destinationRequests.remove(currentFloor);
        }


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

    private Integer findNextAbove() {
        return Stream.concat(pickupRequests.stream(), destinationRequests.stream())
                     .filter(f -> f > currentFloor)
                     .min(Integer::compareTo)
                     .orElse(null);
    }
    
    private Integer findNextBelow() {
        return Stream.concat(pickupRequests.stream(), destinationRequests.stream())
                     .filter(f -> f < currentFloor)
                     .max(Integer::compareTo)
                     .orElse(null);
    }
    public synchronized int getCurrentFloor() {
        return currentFloor;
    }
    
}
