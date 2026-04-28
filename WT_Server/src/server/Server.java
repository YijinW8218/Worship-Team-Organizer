package server;

import model.Calendar;
import model.Event;

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
                }else if (msgFromClient.startsWith("ADD_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length >= 4) {
                        // 索引从 1 开始，跳过第一个（COMMAND）
                        String dateString = parts[1];
                        String timeString = parts[2];
                        String title = parts[3];
                        cl_AddEvent(dateString, timeString, title);
                    }
                }else if (msgFromClient.startsWith("LIST_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length>=2) {
                        //get the events
                        String dateString = parts[1];
                        ArrayList<Event> events = cl_listEvents(dateString);
                        //transfer events list to string
                        System.out.println(events); //仅调试用
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
                    }
                }else if (msgFromClient.startsWith("REMOVE_EVENT")) {
                    String[] parts = msgFromClient.split("\\|");
                    if (parts.length>=2) {
                        //get the titles of the events
                        String title = parts[1];
                        cl_removeEvent(title);
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

    public static void cl_AddEvent(String dateString, String timeString, String title) {
        //1. create the event
        //dateString format:2026-4-21;    timeString format: 14:30
        Event e = new Event( LocalDate.parse(dateString), LocalTime.parse(timeString), title);
        //2. add the event to calendar
        calendar.addEvent(e);
    }

    public static void cl_removeEvent(String title) {
        Event event = calendar.searchByTitle(title);
        calendar.removeEvent(event);
    }

    public static ArrayList<Event> cl_listEvents(String dateString) {
        return calendar.getEvents(LocalDate.parse(dateString));
    }

    public static void shutdown() {
        calendar.save();
        System.out.println("[server.Server] Saved all data before shutdown.");
    }
}