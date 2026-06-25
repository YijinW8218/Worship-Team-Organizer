import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;


public class Client {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        if (System.getProperty("use.cli") == null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    ClientConnection connection = new ClientConnection("localhost", 1234);
                    Login loginFrame = new Login(connection);
                    loginFrame.setVisible(true);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Cannot connect to server: " + ex.getMessage());
                }
            });
            return;
        }

        try {
            Socket socket = new Socket("localhost", 1234);
            socket.setSoTimeout(10000);
            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(inputStreamReader);
            BufferedWriter writer = new BufferedWriter(outputStreamWriter);
            System.out.println("Worship Team Organizer:");

            boolean clientrunning = true;
            boolean running = true;
            boolean running2 = true;

            while (clientrunning) {
                // log in page
                while (running) {
                    printLogin();
                    String choice = scanner.nextLine().trim();

                    //handle login command
                    if (choice.equals("1")) {
                        try {
                            System.out.println("Enter username:");
                            String username = scanner.nextLine().trim();
                            System.out.println("Enter password:");
                            String password = scanner.nextLine().trim();

                            String response = sendCommandForResponse(writer, reader,
                                    "LOGIN|" + username + "|" + password);
                            if ("LOGIN_SUCCESS".equals(response)) {
                                running = false;
                                System.out.println("You have logged in.");
                            } else {
                                System.out.println("Username or password is incorrect.");
                            }
                        } catch (Exception ex) {
                            System.out.println("Try again.");
                        }
                    }

                    //handle register command
                    else if (choice.equals("2")) {
                        try {
                            System.out.println("Enter username:");
                            String username = scanner.nextLine().trim();
                            System.out.println("Enter password:");
                            String password = scanner.nextLine().trim();

                            String response = sendCommandForResponse(writer, reader,
                                    "REGISTER|" + username + "|" + password);
                            if ("REGISTER_SUCCESS".equals(response)) {
                                running = false;
                                System.out.println("You have registered and logged in.");
                            } else {
                                System.out.println("Username already exists.");
                            }
                        } catch (Exception ex) {
                            System.out.println("Try again.");
                        }
                    } else {
                        System.out.println("Invalid input. Try again.");
                    }
                }


                // main menu
                while (running2) {
                    printMenu();
                    String choice = scanner.nextLine().trim();

                    if (choice.equals("1")) { //handle add event
                        try {
                            System.out.print("Enter date (ex.2026-04-06): ");
                            String dateString = scanner.nextLine().trim();
                            System.out.print("Enter time (ex.16:08): ");
                            String timeString = scanner.nextLine().trim();
                            System.out.print("Enter title: ");
                            String title = scanner.nextLine().trim();

                            String response = sendCommandForResponse(writer, reader,
                                    "ADD_EVENT|" + dateString + "|" + timeString + "|" + title);
                            System.out.println(response);
                        } catch (Exception ex) {
                            System.out.println("Invalid format. Try again.");
                        }
                    } else if (choice.equals("2")) {//handle list events
                        try {
                            System.out.print("Enter date (ex.2026-04-06): ");
                            String dateString = scanner.nextLine().trim();

                            String msgReceive = sendCommandForResponse(writer, reader,
                                    "LIST_EVENT|" + dateString);
                            if (msgReceive.startsWith("Events List:")) {
                                printEventList(msgReceive);
                            } else {
                                System.out.println(msgReceive);
                            }
                        } catch (Exception ex) {
                            System.out.println("Invalid format. Try again.");
                        }
                    } else if (choice.equals("3")) { //handle remove event
                        try {
                            System.out.print("Enter the title of the event: ");
                            String title = scanner.nextLine().trim();

                            String response = sendCommandForResponse(writer, reader,
                                    "REMOVE_EVENT|" + title);
                            System.out.println(response);
                        } catch (Exception ex) {
                            System.out.println("Invalid format. Try again.");
                        }
                    } else if (choice.equals("4")) {//handle edit songs
                        try {
                            //first, list events
                            System.out.println("Enter date to see events' titles:(ex.2026-04-06)");
                            String dateString = scanner.nextLine().trim();
                            String msgReceive = sendCommandForResponse(writer, reader,
                                    "LIST_EVENT|" + dateString);
                            if (msgReceive.startsWith("Events List:")) {
                                printEventList(msgReceive);
                            }
                            //second, choose event to add/remove songs
                            System.out.println("Enter the title of event which you want to edit songs");
                            String title = scanner.nextLine().trim();
                            String msgReceive2 = sendCommandForResponse(writer, reader,
                                    "LIST_SONGS|" + title);
                            if (msgReceive2.startsWith("Songs List:")) {
                                printSongList(msgReceive2);
                            }
                            //choose add/remove songs
                            while (true) {
                                System.out.println("1. Add a song");
                                System.out.println("2. Remove a song");
                                System.out.println("Choose an operations:");
                                String c = scanner.nextLine().trim();
                                if (c.equals("1")) { //choose to add a song
                                    System.out.println("Enter the name of the song that you want to add:");
                                    String name = scanner.nextLine().trim();
                                    System.out.println("Enter the author of the song:");
                                    String author = scanner.nextLine().trim();
                                    String response = sendCommandForResponse(writer, reader,
                                            "ADD_SONG|" + title + "|" + name + "|" + author);
                                    System.out.println(response);
                                    break;
                                } else if (c.equals("2")) { //choose to remove a song
                                    System.out.println("Enter the name of the song that you want to remove:");
                                    String name = scanner.nextLine().trim();
                                    String response = sendCommandForResponse(writer, reader,
                                            "REMOVE_SONG|" + title + "|" + name);
                                    System.out.println(response);
                                    break;
                                } else {
                                    System.out.println("Invalid format. Try again.");
                                }
                            }
                        } catch (Exception ex) {
                            System.out.println("Invalid format. Try again.");
                            String c = scanner.nextLine().trim();
                        }
                    } else if (choice.equals("5")) { //handle edit team
                        try {
                            //first, list events
                            System.out.println("Enter date to see events' titles:(ex.2026-04-06)");
                            String dateString = scanner.nextLine().trim();
                            String msgReceive3 = sendCommandForResponse(writer, reader,
                                    "LIST_EVENT|" + dateString);
                            if (msgReceive3.startsWith("Events List:")) {
                                printEventList(msgReceive3);
                            }
                            //second, choose event to add/remove members
                            System.out.println("Enter the title of event which you want to edit the team");
                            String title = scanner.nextLine().trim();
                            String msgReceive4 = sendCommandForResponse(writer, reader,
                                    "LIST_MEMBERS|" + title);
                            if (msgReceive4.startsWith("Members List:")) {
                                System.out.println(msgReceive4);
                            }
                            //choose add/remove members
                            while (true) {
                                System.out.println("1. Add a member");
                                System.out.println("2. Remove a member");
                                System.out.println("Choose an operations:");
                                String c = scanner.nextLine().trim();
                                if (c.equals("1")) { //choose to add a member
                                    System.out.println("Enter the name of the member that you want to add:");
                                    String name = scanner.nextLine().trim();
                                    String response = sendCommandForResponse(writer, reader,
                                            "ADD_MEMBER|" + title + "|" + name);
                                    System.out.println(response);
                                    break;
                                } else if (c.equals("2")) { //choose to remove a member
                                    System.out.println("Enter the name of the member that you want to remove:");
                                    String name = scanner.nextLine().trim();
                                    String response = sendCommandForResponse(writer, reader,
                                            "REMOVE_MEMBER|" + title + "|" + name);
                                    System.out.println(response);
                                    break;
                                } else {
                                    System.out.println("Invalid format. Try again.");
                                }
                            }
                        } catch (Exception ex) {
                            System.out.println("Invalid format. Try again.");
                            String c = scanner.nextLine().trim();
                        }
                    } else if (choice.equals("0")) {
                        running2 = false;
                        System.out.println("bye.");
                        break;
                    } else {
                        System.out.println("Invalid input. Try again.");
                    }
                }
            }
        }catch (IOException e) {e.printStackTrace();}
    }

    private static String sendCommandForResponse(
            BufferedWriter writer,
            BufferedReader reader,
            String command
    ) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
        String response = reader.readLine();
        if (response == null) {
            throw new IOException("Server disconnected.");
        }
        return response;
    }


    public static void printEventList(String msgReceive) {
        String content = msgReceive.substring("Events List:".length()); //skip the title of the data
        String[] eventStrings = content.split("\\|");

        System.out.println("Events List:");
        for (String e : eventStrings) {
            if (!e.isBlank()) {
                System.out.println(e.trim());
            }
        }
    }

    public static void printSongList(String msgReceive) {
        String content = msgReceive.substring("Songs List:".length()); //skip the title of the data
        String[] songStrings = content.split("\\|");

        System.out.println("Songs List:");
        for (String s : songStrings) {
            if (!s.isBlank()) {
                System.out.println(s.trim());
            }
        }
    }







    private static void printLogin() {
        System.out.println("Welcome to WT.");
        System.out.println("1. Log in");
        System.out.println("2. Register (if you are a new member)");
        System.out.println("Enter");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("Enter an operation:");
        System.out.println("1. Add an event");
        System.out.println("2. Check the events in a date");
        System.out.println("3. Delete an event");
        System.out.println("4. Edit songs");
        System.out.println("5. Edit team");
        System.out.println("0. Exit");
        System.out.print("Enter：");
    }

    public static String getGUIdata(String data) {
        return data;
    }



}
