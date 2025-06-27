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
    public synchronized boolean isMovingToward(int floor) {
        if (pickupRequests.isEmpty() && destinationRequests.isEmpty()) return false;
    
        if (goingUp && floor >= currentFloor) return true;
        if (!goingUp && floor <= currentFloor) return true;
    
        return false;
    }
    public synchronized int getTotalRequests() {
        return pickupRequests.size() + destinationRequests.size();
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
        waitingForDestination = false; // clear the block
        notifyAll(); // 💡 crucial to unblock thread
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
        if (!inMaintenance) notifyAll();
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
                wait(3000);
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
                while (inMaintenance) wait();

                if (!hasRequests()) claimNearbyPickup();

                while (!hasRequests()) wait();

                if (shouldStopAtCurrentFloor()) {
                    processStop();

                    if (waitingForDestination) {
                        System.out.println("Elevator " + name + " waiting for destination at floor " + currentFloor);

                        long startTime = System.currentTimeMillis();

                        while (waitingForDestination && System.currentTimeMillis() - startTime < 8000) {
                            wait(1000); // wait in 1 sec intervals, respond early if destination is added
                        }

                        // Either timeout or input received
                        waitingForDestination = false;
                        System.out.println("Elevator " + name + " resumes after destination wait or timeout.");
                    }
                }

                // ✅ Block completely before moving if waitingForDestination is true
                while (waitingForDestination) {
                    wait(1000); // pause and re-check every second
                }

                Integer nextFloor = getNextFloor();
                if (nextFloor != null) {
                    goingUp = nextFloor > currentFloor;

                    while (currentFloor != nextFloor) {
                        if (inMaintenance || waitingForDestination) break;

                        currentFloor += goingUp ? 1 : -1;
                        System.out.println("Elevator " + name + " moving to floor: " + currentFloor);

                        if (shouldStopAtCurrentFloor()) {
                            processStop();

                            if (waitingForDestination) {
                                long startTime = System.currentTimeMillis();
                                while (waitingForDestination && System.currentTimeMillis() - startTime < 8000) {
                                    wait(1000);
                                }
                                waitingForDestination = false;
                                break;
                            }
                        }

                        wait(1000);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


    private synchronized Integer getNextFloor() {
        TreeSet<Integer> all = new TreeSet<>();
        all.addAll(pickupRequests);
        all.addAll(destinationRequests);
        if (all.isEmpty()) return null;

        TreeSet<Integer> priority = new TreeSet<>();
        for (int floor : all) {
            if (Math.abs(floor - currentFloor) <= 10) priority.add(floor);
        }

        TreeSet<Integer> target = !priority.isEmpty() ? priority : all;

        if (goingUp) {
            Integer higher = target.ceiling(currentFloor + 1);
            return (higher != null) ? higher : target.floor(currentFloor - 1);
        } else {
            Integer lower = target.floor(currentFloor - 1);
            return (lower != null) ? lower : target.ceiling(currentFloor + 1);
        }
    }
}
