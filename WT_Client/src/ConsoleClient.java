import java.sql.SQLOutput;
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
                case "4":
                    editSongsUI(client);
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
        System.out.println("4. Edit songs");
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

    private static void editSongsUI(Client client) {
        try {
            //first, list events
            System.out.println("Enter date to see events' titles:(ex.2026-04-06)");
            String dateString = scanner.nextLine().trim();
            String command_msg = "LIST_EVENT|" + dateString;
            client.send(command_msg);
                // Client will listen to server and print the list.

            //second, choose event to add/remove songs
            System.out.println("Enter the title of event which you want to edit songs");
            String title = scanner.nextLine().trim();
            String command_msg2 = "LIST_SONGS|" + title;
            client.send(command_msg2);
                // Client will listen to server and print the list.
            //choose add/remove songs
            while (true) {
                System.out.println("1. Add a song");
                System.out.println("2. Remove a song");
                System.out.println("Choose an operations:");
                String choice = scanner.nextLine().trim();
                if (choice.equals("1")) { //choose to add a song
                    System.out.println("Enter the name of the song that you want to add:");
                    String name = scanner.nextLine().trim();
                    System.out.println("Enter the author of the song:");
                    String author = scanner.nextLine().trim();
                    String command_msg3 = "ADD_SONG|" + title  + "|" + name  + "|" + author;
                    client.send(command_msg3);
                    break;
                }else if (choice.equals("2")) { //choose to remove a song
                    System.out.println("Enter the name of the song that you want to remove:");
                    String name = scanner.nextLine().trim();
                    String command_msg4 = "REMOVE_SONG|" + title  + "|" + name;
                    client.send(command_msg4);
                    break;
                }else {
                    System.out.println("Invalid format. Try again.");
                }
            }

        } catch (Exception ex) {
            System.out.println("Invalid format. Try again.");
            String choice = scanner.nextLine().trim();
        }
    }
}
