import model.Calendar;
import model.Event;
import model.Member;
import model.Song;
import storage.UserActivity;
import storage.UsersInfo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;


public class Server {
    private static Calendar calendar = new Calendar();
    private static UsersInfo usersInfo = new UsersInfo();
    private static UserActivity userActivity = new UserActivity();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Waiting for the client request...");
            Socket client = serverSocket.accept();
            System.out.println("Client connected:" +client);
            String currentUser = null;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

            while (true) {
                String msgFromClient = bufferedReader.readLine(); //receive client
                if (msgFromClient == null) {
                    System.out.println("Client disconnected.");
                    break;
                }else {
                    msgFromClient = msgFromClient.trim(); //space and empty lines
                    System.out.println(client + ":" + msgFromClient);
                }



                if ("QUIT".equals(msgFromClient)) {
                    client.close();
                    System.out.println("Client disconnected.");
                    break;
                }


                else if (msgFromClient.startsWith("LOGIN")) {
                    String[] parts = msgFromClient.split("\\|");
                    String username = parts[1];
                    String password = parts[2];

                    if (usersInfo.login(username, password)) { //check if username exists and password is right
                        currentUser = username; //record the current username
                        System.out.println("Successfully logged in.");
                    } else {
                        System.out.println("Fail to log in. Try again.");
                    }
                }


                else if (msgFromClient.startsWith("REGISTER")) {
                    String[] parts = msgFromClient.split("\\|");
                    String username = parts[1];
                    String password = parts[2];
                    if (usersInfo.register(username, password)) { //check if username repeated
                        currentUser = username; //record the current username
                        System.out.println("Successfully register. Welcome.");
                    } else {
                        System.out.println("Username already exists. Try again with another username.");
                    }
                }


                else if (msgFromClient.startsWith("ADD_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length == 4) {
                        String dateString = parts[1];
                        String timeString = parts[2];
                        String title = parts[3];
                        boolean success = cl_AddEvent(dateString, timeString, title);
                        if (success) {
                            System.out.println("A new event has been added.");
                        }else{
                            System.out.println("Repeated event's title. Fail to add event.");
                        }
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("LIST_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==2) {
                        String dateString = parts[1];
                        ArrayList<Event> events = cl_listEvents(dateString);
                        //transfer events list to string
                        StringBuilder sb = new StringBuilder();
                        sb.append("Events List:");
                        for (Event e : events) {
                            String e_string = e.toString() + "|";
                            sb.append(e_string);
                        }
                        String events_string = sb.toString();
                        //send the events list to client
                        bufferedWriter.write(events_string);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("REMOVE_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==2) {
                        //get the titles of the events
                        String title = parts[1];
                        cl_removeEvent(title);
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("ADD_SONG")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==4) {
                        String eventTitle = parts[1];
                        String name = parts[2];
                        String author = parts[3];
                        cl_AddSong(eventTitle, name, author);
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("REMOVE_SONG")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==3) {
                        String eventTitle = parts[1];
                        String name = parts[2];
                        cl_RemoveSong(eventTitle, name);
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("LIST_SONGS")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==2) {
                        String eventTitle = parts[1];
                        ArrayList<Song> songs = cl_ListSong(eventTitle);
                        //transfer songs list to string
                        StringBuilder sb = new StringBuilder();
                        sb.append("Songs List:");
                        for (Song s : songs) {
                            String s_string = s.toString() + "|";
                            sb.append(s_string);
                        }
                        String songs_string = sb.toString();
                        //send the songs list to client
                        bufferedWriter.write(songs_string);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("ADD_MEMBER")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==3) {
                        String eventTitle = parts[1];
                        String memberName = parts[2];

                        Event event = calendar.searchByTitle(eventTitle);
                        Member member = usersInfo.getMemberByName(memberName);

                        if (event != null && member != null) {
                            event.addtoTeam(member); // add this member to event.team
                            member.addEvent(event);  // add this event to member.schedule
                        }
                        calendar.save(); //update
                        usersInfo.save(); //update
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("REMOVE_MEMBER")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==3) {
                        String eventTitle = parts[1];
                        String memberName = parts[2];

                        Event event = calendar.searchByTitle(eventTitle);
                        Member member = usersInfo.getMemberByName(memberName);

                        if (event != null && member != null) {
                            event.removefromTeam(member);  // remove this member from event.team
                            member.removeEvent(event);   // remove this event from member.schedule
                        }
                        calendar.save(); //update
                        usersInfo.save(); //update
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else if (msgFromClient.startsWith("LIST_MEMBERS")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==2) {
                        String eventTitle = parts[1];
                        Event event = calendar.searchByTitle(eventTitle);
                        if (event != null) {
                            StringBuilder sb = new StringBuilder("Members List:");
                            for (Member m : event.getTeam()) {
                                sb.append(m.getUserName()).append("|");
                            }
                            String members_string = sb.toString();
                            //send the members list to client
                            bufferedWriter.write(members_string);
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else{
                    System.out.println("invalid command");
                }
            }

            calendar.save();
            usersInfo.save();
            System.out.println("Server saved all data.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        }












    public static boolean cl_AddEvent(String dateString, String timeString, String title) {
        boolean success = false;
        // don't create event if title repeat
        if (calendar.searchByTitle(title) == null) {
            LocalDate date = LocalDate.parse(dateString);
            LocalTime time = LocalTime.parse(timeString);
            Event event = new Event(date, time, title);
            calendar.addEvent(event);
            success = true;
        }
        return success;
    }

    public static void cl_removeEvent(String title) {
        Event event = calendar.searchByTitle(title);
        calendar.removeEvent(event);
    }

    public static ArrayList<Event> cl_listEvents(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        return calendar.getEvents(date);
    }

    public static void cl_AddSong(String eventTitle, String name, String author) {
        Event event = calendar.searchByTitle(eventTitle);
        event.addSong(name, author);
    }

    public static void cl_RemoveSong(String eventTitle, String name) {
        Event event = calendar.searchByTitle(eventTitle);
        event.removeSong(name);
    }

    public static ArrayList<Song> cl_ListSong(String eventTitle) {
        Event event = calendar.searchByTitle(eventTitle);
        ArrayList<Song> songs = event.getSongs();
        return songs;
    }
}