import java.util.*;

public class Elevatorr implements Runnable {
    private final String name;
    private boolean inMaintenance = false;

    private int currentFloor = 0;
    private boolean goingUp = true;
    private boolean waitingForDestination = false;
    private final int maxFloor;

    private final TreeSet<Integer> pickupRequests = new TreeSet<>();
    private final TreeSet<Integer> destinationRequests = new TreeSet<>();
    private final List<Integer> globalPickupRequests;

    public Elevatorr(String name, int maxFloor, List<Integer> globalPickupRequests) {
        this.name = name;
        this.maxFloor = maxFloor;
        this.globalPickupRequests = globalPickupRequests;
    }

    public synchronized void addPickupRequest(int floor) {
        if (!inMaintenance && floor >= 0 && floor <= maxFloor) {
            pickupRequests.add(floor);
            notifyAll();
        }
    }

    public synchronized void addDestinationRequest(int floor) {
        if (!inMaintenance && floor >= 0 && floor <= maxFloor) {
            destinationRequests.add(floor);
            waitingForDestination = false;
            notifyAll();
        }
    }

    public synchronized int getCurrentFloor() {
        return currentFloor;
    }

    public synchronized boolean isWaitingForDestination() {
        return waitingForDestination;
    }

    public synchronized boolean isInMaintenance() {
        return inMaintenance;
    }

    public synchronized void setInMaintenance(boolean inMaintenance) {
        this.inMaintenance = inMaintenance;
        if (!inMaintenance) {
            notifyAll(); // Resume thread if it's re-enabled
        }
    }

    private synchronized boolean hasRequests() {
        return !pickupRequests.isEmpty() || !destinationRequests.isEmpty();
    }

    private synchronized boolean shouldStopAtCurrentFloor() {
        return pickupRequests.contains(currentFloor) || destinationRequests.contains(currentFloor);
    }

    private synchronized void processStop() {
        boolean stopped = false;

        if (pickupRequests.remove(currentFloor)) {
            System.out.println("Elevator " + name + " picked up at floor: " + currentFloor);
            waitingForDestination = true;
            stopped = true;
        }

        if (destinationRequests.remove(currentFloor)) {
            System.out.println("Elevator " + name + " dropped off at floor: " + currentFloor);
            stopped = true;
        }

        if (stopped) {
            try {
                wait(3000); // pause at the floor for 3 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void claimNearbyPickup() {
        synchronized (globalPickupRequests) {
            Iterator<Integer> iterator = globalPickupRequests.iterator();
            while (iterator.hasNext()) {
                int floor = iterator.next();
                if (Math.abs(floor - currentFloor) <= 3) {
                    synchronized (this) {
                        pickupRequests.add(floor);
                        notifyAll();
                    }
                    iterator.remove();
                    break;
                }
            }
        }
    }

    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    // 🚧 Wait here if in maintenance mode
                    while (inMaintenance) {
                        wait(); // Suspended while in maintenance
                    }

                    // Take from global queue if idle
                    if (!hasRequests()) {
                        claimNearbyPickup();
                    }

                    while (!hasRequests()) {
                        wait(); // nothing to do
                    }

                    // Handle floor stop if needed
                    if (shouldStopAtCurrentFloor()) {
                        processStop();

                        // If it's a pickup, allow user to enter destination
                        if (waitingForDestination) {
                            wait(10000); // wait for destination input
                            waitingForDestination = false;
                        }
                    }

                    Integer nextFloor = getNextFloor();
                    if (nextFloor != null) {
                        goingUp = nextFloor > currentFloor;

                        while (currentFloor != nextFloor) {
                            if (inMaintenance) break; // if maintenance is triggered mid-move

                            currentFloor += goingUp ? 1 : -1;
                            System.out.println("Elevator " + name + " moving... Floor: " + currentFloor);

                            if (shouldStopAtCurrentFloor()) {
                                processStop();
                                if (waitingForDestination) {
                                    wait(10000); // wait at that floor
                                    waitingForDestination = false;
                                    break; // exit loop to re-check queue
                                }
                            }

                            wait(1000); // simulate time per floor
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized Integer getNextFloor() {
        TreeSet<Integer> allRequests = new TreeSet<>();
        allRequests.addAll(pickupRequests);
        allRequests.addAll(destinationRequests);

        if (goingUp) {
            Integer higher = allRequests.ceiling(currentFloor + 1);
            if (higher != null) return higher;
            return allRequests.floor(currentFloor - 1);
        } else {
            Integer lower = allRequests.floor(currentFloor - 1);
            if (lower != null) return lower;
            return allRequests.ceiling(currentFloor + 1);
        }
    }
}
