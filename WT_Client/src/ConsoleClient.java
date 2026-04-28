import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

public class ConsoleClient {
    private static final Scanner scanner = new Scanner(System.in);

    public static void start(Client client) {
        System.out.println("Worship Team Organizer:");

        boolean running = true;

        while (running) {
            printMenu(); //TODO:(bug) UI printing menu before printing message from Server
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    addEventUI(client);
                    break;
                case "2":
                    listEventsUI(client);
                    break;
                case "3":
                    removeEventUI(client);
                    break;
                case "0":
                    running = false;
                    System.out.println("bye.");
                    break;
                default:
                    System.out.println("Invalid input. Try again.");
            }
        }
    }
    private static void printMenu() {
        System.out.println();
        System.out.println("Enter an operation:");
        System.out.println("1. Add an event");
        System.out.println("2. Check the events in a date");
        System.out.println("3. Delete an event");
        System.out.println("0. Exit");
        System.out.print("Enter：");
    }


    private static void addEventUI(Client client) {
        try {
            //Client send command to Server
            //dateString format:2026-4-21;    timeString format: 14:30
            System.out.print("Enter date (ex.2026-04-06): ");
            String dateString = scanner.nextLine().trim();
            System.out.print("Enter time (ex.16:08): ");
            String timeString = scanner.nextLine().trim();
            System.out.print("Enter title: ");
            String title = scanner.nextLine().trim();

            String command_msg = "ADD_EVENT|" + dateString + "|" + timeString + "|" + title;
            client.send(command_msg);

            System.out.println("Event has been added.");

        } catch (Exception ex) {
            System.out.println("Invalid format. Try again.");  //todo: add input test
        }
    }

    private static void listEventsUI(Client client) {
        try {
            System.out.print("Enter date (ex.2026-04-06): ");
            String dateString = scanner.nextLine().trim();

            String command_msg = "LIST_EVENT|" + dateString;
            client.send(command_msg);
            // Client will listen to server and print the list.

        } catch (Exception ex) {
            System.out.println("Invalid format. Try again.");
        }
    }

    private static void removeEventUI(Client client) {
        try {
            System.out.print("Enter the title of the event: ");
            String title = scanner.nextLine().trim();

            String command_msg = "REMOVE_EVENT|" + title;
            client.send(command_msg);
            //server will receive and delete the event
        } catch (Exception ex) {
            System.out.println("Invalid format. Try again.");
        }
    }
}
