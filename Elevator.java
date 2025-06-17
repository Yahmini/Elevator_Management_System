import java.util.*;

public class ElevatorSimulation {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Person> people = new ArrayList<>();

        System.out.print("Enter number of people: ");
        int n = scanner.nextInt();

        for (int i = 1; i <= n; i++) {
            System.out.println("\nPerson " + i + ":");
            System.out.print("Enter source floor (0-10): ");
            int source = scanner.nextInt();
            System.out.print("Enter destination floor (0-10): ");
            int dest = scanner.nextInt();

            if (source == dest) {
                System.out.println("Source and destination cannot be same! Skipping person.");
                continue;
            }

            people.add(new Person(i, source, dest));
        }

        System.out.print("\nEnter current floor of the elevator (0-10): ");
        int startFloor = scanner.nextInt();

        Elevator elevator = new Elevator(people, startFloor);
        scanner.close();
        elevator.run();
    }
}

class Person {
    int id;
    int source;
    int destination;
    boolean pickedUp = false;
    boolean delivered = false;

    public Person(int id, int source, int destination) {
        this.id = id;
        this.source = source;
        this.destination = destination;
    }

    public boolean goingUp() {
        return destination > source;
    }

    public String toString() {
        return "Person " + id + " (" + source + " -> " + destination + ")";
    }
}

class Elevator {
    int currentFloor;
    boolean goingUp = true;
    List<Person> people;
    List<Person> onboard = new ArrayList<>();

    public Elevator(List<Person> people, int startFloor) {
        this.people = people;
        this.currentFloor = startFloor;
    }

    public void run() {
        System.out.println("\n Starting elevator simulation...");
        setInitialDirection();

        while (!allServed()) {
            System.out.println("\n Current floor: " + currentFloor);

            Iterator<Person> onboardIt = onboard.iterator();
            while (onboardIt.hasNext()) {
                Person p = onboardIt.next();
                if (p.destination == currentFloor) {
                    System.out.println(" Dropping off " + p);
                    onboardIt.remove();
                    p.delivered = true;
                }
            }
            for (Person p : people) {
                if (!p.pickedUp && !p.delivered && p.source == currentFloor && p.goingUp() == goingUp) {
                    System.out.println(" Picking up " + p);
                    onboard.add(p);
                    p.pickedUp = true;
                }
            }
            if (!hasMoreRequestsInDirection()) {
                if (hasAnyPendingRequests()) {
                    goingUp = !goingUp;
                    System.out.println(" Changing direction to " + (goingUp ? "UP" : "DOWN"));
                } else {
                    break; 
                }
            }

            
            currentFloor += goingUp ? 1 : -1;

            if (currentFloor >= 10) {
                currentFloor = 10;
                goingUp = false;
            } else if (currentFloor <= 0) {
                currentFloor = 0;
                goingUp = true;
            }
        }

        System.out.println("\n All people served. Simulation complete.");
    }

    private boolean allServed() {
        for (Person p : people) {
            if (!p.delivered) return false;
        }
        return true;
    }

    private boolean hasAnyPendingRequests() {
        for (Person p : people) {
            if (!p.pickedUp || !p.delivered) return true;
        }
        return false;
    }

    private boolean hasMoreRequestsInDirection() {
        for (Person p : onboard) {
            if ((goingUp && p.destination > currentFloor) || (!goingUp && p.destination < currentFloor)) {
                return true;
            }
        }

        for (Person p : people) {
            if (!p.pickedUp && !p.delivered && p.source == currentFloor && p.goingUp() == goingUp) return true;
            if (!p.pickedUp && !p.delivered &&
                ((goingUp && p.source > currentFloor) || (!goingUp && p.source < currentFloor))) return true;
        }

        return false;
    }

    private void setInitialDirection() {
        for (Person p : people) {
            if (p.source == currentFloor) {
                goingUp = p.goingUp();
                System.out.println(" Person waiting at current floor. Initial direction set to " + (goingUp ? "UP" : "DOWN"));
                return;
            }
        }

        int minDistance = Integer.MAX_VALUE;
        boolean direction = true;
        for (Person p : people) {
            int distance = Math.abs(p.source - currentFloor);
            if (distance < minDistance) {
                minDistance = distance;
                direction = p.source > currentFloor;
            }
        }

        goingUp = direction;
        System.out.println(" Initial direction set to " + (goingUp ? "UP" : "DOWN"));
    }
}

