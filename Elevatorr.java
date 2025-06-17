import java.util.*;

public class Elevator implements Runnable {
    private int currentFloor = 0;
    private boolean goingUp = true;
    private final PriorityQueue<Integer> upRequests = new PriorityQueue<>();
    private final PriorityQueue<Integer> downRequests = new PriorityQueue<>(Collections.reverseOrder());

    public synchronized void addRequest(int floor) {
        if (floor > currentFloor) {
            upRequests.add(floor);
        } else if (floor < currentFloor) {
            downRequests.add(floor);
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(1000); // simulate time between floors
                processRequests();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void processRequests() {
        System.out.println("Elevator at floor: " + currentFloor);
        if (goingUp) {
            if (!upRequests.isEmpty()) {
                int next = upRequests.peek();
                if (currentFloor < next) {
                    currentFloor++;
                } else {
                    System.out.println("Picking/Dropping person at floor: " + currentFloor);
                    upRequests.poll();
                }
            } else if (!downRequests.isEmpty()) {
                goingUp = false;
            }
        } else {
            if (!downRequests.isEmpty()) {
                int next = downRequests.peek();
                if (currentFloor > next) {
                    currentFloor--;
                } else {
                    System.out.println("Picking/Dropping person at floor: " + currentFloor);
                    downRequests.poll();
                }
            } else if (!upRequests.isEmpty()) {
                goingUp = true;
            }
        }
    }
}

