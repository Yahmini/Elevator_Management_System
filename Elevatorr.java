// Elevatorr.java
import java.util.*;

public class Elevatorr implements Runnable {
    private int currentFloor = 0;
    private boolean goingUp = true;
    private final int maxFloor;

    private final TreeSet<Integer> pickupRequests = new TreeSet<>();
    private final TreeSet<Integer> destinationRequests = new TreeSet<>();

    public Elevatorr(int maxFloor) {
        this.maxFloor = maxFloor;
    }

    public synchronized void addPickupRequest(int floor) {
        if (floor >= 0 && floor <= maxFloor) {
            pickupRequests.add(floor);
            notifyAll();
        }
    }

    public synchronized void addDestinationRequest(int floor) {
        if (floor >= 0 && floor <= maxFloor) {
            destinationRequests.add(floor);
            notifyAll();
        }
    }

    public synchronized int getCurrentFloor() {
        return currentFloor;
    }

    private synchronized boolean hasRequests() {
        return !pickupRequests.isEmpty() || !destinationRequests.isEmpty();
    }

    private synchronized boolean shouldStopAtCurrentFloor() {
        return pickupRequests.contains(currentFloor) || destinationRequests.contains(currentFloor);
    }

    private synchronized void processStop() {
        if (pickupRequests.remove(currentFloor)) {
            System.out.println("Pickup at floor: " + currentFloor);
        }

        if (destinationRequests.remove(currentFloor)) {
            System.out.println("Drop-off at floor: " + currentFloor);
        }
    }

    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    while (!hasRequests()) wait();
                }

                Thread.sleep(1500);

                synchronized (this) {
                    if (shouldStopAtCurrentFloor()) {
                        processStop();
                    }

                    Integer nextFloor = getNextFloor();
                    if (nextFloor != null) {
                        goingUp = nextFloor > currentFloor;
                        currentFloor += goingUp ? 1 : -1;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Integer getNextFloor() {
        TreeSet<Integer> allRequests = new TreeSet<>();
        allRequests.addAll(pickupRequests);
        allRequests.addAll(destinationRequests);

        if (goingUp) {
            return allRequests.ceiling(currentFloor + 1) != null ? allRequests.ceiling(currentFloor + 1)
                                                                  : allRequests.floor(currentFloor - 1);
        } else {
            return allRequests.floor(currentFloor - 1) != null ? allRequests.floor(currentFloor - 1)
                                                               : allRequests.ceiling(currentFloor + 1);
        }
    }
}
