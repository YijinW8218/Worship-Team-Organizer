package server;

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
    private static Socket socket = null;
    private static ServerSocket serverSocket = null;

    private static Calendar calendar = new Calendar();
    private static UsersInfo usersInfo = new UsersInfo();
    private static UserActivity userActivity = new UserActivity();

    public static void listenClient(int port) throws IOException{
        serverSocket = new ServerSocket(port);
        System.out.println("Waiting for the client request...");
        while (true) {
            //create socket and waiting for client connection
            Socket clsoc = serverSocket.accept(); // client socket
            new Thread(()->HandleClient(clsoc)).start();
        }
    }

    private static void HandleClient(Socket socket) {
        try{
            System.out.println("Client connected:" + socket);
            String currentUser = null;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            while (true) {
                //chat with the Client
                String msgFromClient = bufferedReader.readLine();
                //first check if client manually disconnected
                if (msgFromClient == null) {
                    System.out.println("Client disconnected.");
                    break;
                }else { msgFromClient = msgFromClient.trim(); } //cancel space and empty lines
                //record the msg from the client
                System.out.println(socket + ":" + msgFromClient);

                //check if client asked for quit
                if ("QUIT".equals(msgFromClient)) {
                    socket.close();
                    System.out.println("Client disconnected.");
                    break;
                } // operate LOGIN command
                else if (msgFromClient.startsWith("LOGIN")) {
                    String[] parts = msgFromClient.split("\\|");
                    String username = parts[1];
                    String password = parts[2];

                    if (usersInfo.login(username, password)) { //check if username exists and password is right
                        currentUser = username; //record the current username
                        bufferedWriter.write("Successfully logged in.");
                    } else {
                        bufferedWriter.write("Fail to log in. Try again.");
                    }
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    continue;
                } //operate REGISTER command
                else if (msgFromClient.startsWith("REGISTER")) {
                    String[] parts = msgFromClient.split("\\|");
                    String username = parts[1];
                    String password = parts[2];
                    if (usersInfo.register(username, password)) { //check if username repeated
                        currentUser = username; //record the current username
                        bufferedWriter.write("Successfully register. Welcome.");
                    } else {
                        bufferedWriter.write("Username already exists. Try again with another username.");
                    }
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                    continue;
                } //operate ADD_EVENT command
                else if (msgFromClient.startsWith("ADD_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length == 4) {
                        String dateString = parts[1];
                        String timeString = parts[2];
                        String title = parts[3];
                        boolean success = cl_AddEvent(dateString, timeString, title);
                        if (success) {
                            bufferedWriter.write("A new event has been added.");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }else{
                            bufferedWriter.write("Repeated event's title. Fail to add event.");
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                } // operate LIST_EVENT command
                else if (msgFromClient.startsWith("LIST_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==2) {
                        //get the events
                        String dateString = parts[1];
                        ArrayList<Event> events = cl_listEvents(dateString);
                        //transfer events list to string
                        StringBuilder sb = new StringBuilder();
                        sb.append("Events List:");
                        for (Event e : events) {
                            String e_string = e.toString() + "|"; //client will split and change rows at |
                            sb.append(e_string);
                        }
                        String events_string = sb.toString();
                        //send the events list in string to client
                        bufferedWriter.write(events_string);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                } // operate REMOVE_EVENT command
                else if (msgFromClient.startsWith("REMOVE_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==2) {
                        //get the titles of the events
                        String title = parts[1];
                        cl_removeEvent(title);
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                } // operate ADD_SONG command
                else if (msgFromClient.startsWith("ADD_SONG")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==4) {
                        String eventTitle = parts[1];
                        String name = parts[2];
                        String author = parts[3];
                        cl_AddSong(eventTitle, name, author);
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                } // operate REMOVE_SONG command
                else if (msgFromClient.startsWith("REMOVE_SONG")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==3) {
                        String eventTitle = parts[1];
                        String name = parts[2];
                        cl_RemoveSong(eventTitle, name);
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                } // operate LIST_SONGS command
                else if (msgFromClient.startsWith("LIST_SONGS")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length==2) {
                        String eventTitle = parts[1];
                        ArrayList<Song> songs = cl_ListSong(eventTitle);
                        //transfer songs list to string
                        StringBuilder sb = new StringBuilder();
                        sb.append("Songs List:");
                        for (Song s : songs) {
                            String s_string = s.toString() + "|"; //client will split and change rows at |
                            sb.append(s_string);
                        }
                        String songs_string = sb.toString();
                        //send the songs list in string to client
                        bufferedWriter.write(songs_string);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                } // operate ADD_MEMBER to event.team
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
                } // operate REMOVE_MEMBER from event.team
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
                } //operate LIST_MEMBER from event.team
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
                            //send the members list in string to client
                            bufferedWriter.write(members_string);
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }
                        userActivity.logUserActivity(currentUser, msgFromClient); //update userActivity
                    }
                }


                else{
                    String msgToSend = "invalid command";
                    bufferedWriter.write(msgToSend);
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean cl_AddEvent(String dateString, String timeString, String title) {
        boolean success = false;
        // don't create event if title repeat
        if (calendar.searchByTitle(title) == null) {
            //create the event
            //dateString format:2026-4-21;    timeString format: 14:30
            Event e = new Event(LocalDate.parse(dateString), LocalTime.parse(timeString), title);
            //add the event to calendar
            calendar.addEvent(e);
            success = true;
        }
        return success;
    }

    public static void cl_removeEvent(String title) {
        Event event = calendar.searchByTitle(title);
        calendar.removeEvent(event);
    }

    public static ArrayList<Event> cl_listEvents(String dateString) {
        return calendar.getEvents(LocalDate.parse(dateString));
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

    public static void shutdown() {
        calendar.save();
        System.out.println("[server.Server] Saved all data before shutdown.");
    }
}