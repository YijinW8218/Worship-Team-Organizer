import model.Calendar;
import model.Event;
import model.Member;
import model.Song;
import storage.UserActivity;
import storage.UsersInfo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class Server {
    private static Calendar calendar = new Calendar();
    private static UsersInfo usersInfo = new UsersInfo();
    private static UserActivity userActivity = new UserActivity();
    private static final Object DATA_LOCK = new Object();
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1234)) {
            System.out.println("Multi-client server started on port 1234. Waiting for clients...");
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("[SERVER] Accepted client " + client);
                ClientHandler handler = new ClientHandler(client);
                clients.add(handler);
                System.out.println("[SERVER] Online clients: " + clients.size());
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcastDataChanged(String sourceCommand) {
        String message = "DATA_CHANGED|" + encode(sourceCommand);
        for (ClientHandler client : clients) {
            if (client.isSubscribedToChanges()) {
                client.sendLine(message);
            }
        }
    }

    private static CommandResult handleCommand(String msgFromClient, ClientHandler clientHandler) {
        synchronized (DATA_LOCK) {
            String currentUser = clientHandler.getCurrentUser();

            if ("SUBSCRIBE_CHANGES".equals(msgFromClient)) {
                clientHandler.subscribeToChanges();
                return new CommandResult("SUBSCRIBE_CHANGES_SUCCESS", false);
            }

            if (msgFromClient.startsWith("LOGIN")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length < 3) {
                    return new CommandResult("LOGIN_FAIL", false);
                }
                String username = parts[1];
                String password = parts[2];

                if (usersInfo.login(username, password)) {
                    clientHandler.setCurrentUser(username);
                    System.out.println("Successfully logged in.");
                    return new CommandResult("LOGIN_SUCCESS", false);
                }
                System.out.println("Fail to log in. Try again.");
                return new CommandResult("LOGIN_FAIL", false);
            }

            if (msgFromClient.startsWith("REGISTER")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length < 3) {
                    return new CommandResult("REGISTER_FAIL", false);
                }
                String username = parts[1];
                String password = parts[2];
                if (usersInfo.register(username, password)) {
                    clientHandler.setCurrentUser(username);
                    System.out.println("Successfully registered user: " + username);
                    return new CommandResult("REGISTER_SUCCESS", false);
                }
                System.out.println("Username already exists. Try again with another username.");
                return new CommandResult("REGISTER_FAIL", false);
            }

            if (msgFromClient.startsWith("ADD_EVENT")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length == 4) {
                    boolean success = cl_AddEvent(parts[1], parts[2], parts[3]);
                    if (success) {
                        System.out.println("A new event has been added.");
                        userActivity.logUserActivity(currentUser, msgFromClient);
                        return new CommandResult("ADD_EVENT_SUCCESS", true);
                    }
                    System.out.println("Repeated event's title. Fail to add event.");
                }
                return new CommandResult("ADD_EVENT_FAIL", false);
            }

            if (msgFromClient.startsWith("LIST_EVENT")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length == 2) {
                    ArrayList<Event> events = cl_listEvents(parts[1]);
                    StringBuilder sb = new StringBuilder("Events List:");
                    for (Event e : events) {
                        sb.append(e).append("|");
                    }
                    return new CommandResult(sb.toString(), false);
                }
            }

            if (msgFromClient.startsWith("LIST_DAY_DETAIL")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length == 2) {
                    return new CommandResult(buildDayDetailResponse(cl_listEvents(parts[1])), false);
                }
            }

            if (msgFromClient.startsWith("LIST_MONTH_COUNTS")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length == 2) {
                    return new CommandResult(buildMonthCountsResponse(YearMonth.parse(parts[1])), false);
                }
            }

            if (msgFromClient.startsWith("LIST_USER_ACTIVITY")) {
                return new CommandResult(buildUserActivityResponse(), false);
            }

            if (msgFromClient.startsWith("REMOVE_EVENT")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length == 2) {
                    boolean success = cl_removeEvent(parts[1]);
                    if (success) {
                        userActivity.logUserActivity(currentUser, msgFromClient);
                        return new CommandResult("REMOVE_EVENT_SUCCESS", true);
                    }
                }
                return new CommandResult("REMOVE_EVENT_FAIL", false);
            }

            if (msgFromClient.startsWith("EDIT_TOPIC")) {
                String[] parts = msgFromClient.split("\\|", -1);
                if (parts.length == 3) {
                    boolean success = cl_EditTopic(parts[1], decode(parts[2]));
                    if (success) {
                        userActivity.logUserActivity(currentUser, msgFromClient);
                        return new CommandResult("EDIT_TOPIC_SUCCESS", true);
                    }
                }
                return new CommandResult("EDIT_TOPIC_FAIL", false);
            }

            if (msgFromClient.startsWith("ADD_SONG")) {
                String[] parts = msgFromClient.split("\\|", -1);
                if (parts.length == 4) {
                    String eventDate = cl_GetEventDateString(parts[1]);
                    boolean success = cl_AddSong(parts[1], parts[2], parts[3]);
                    if (success) {
                        userActivity.logUserActivity(currentUser, msgFromClient + "|" + eventDate);
                        return new CommandResult("ADD_SONG_SUCCESS", true);
                    }
                }
                return new CommandResult("ADD_SONG_FAIL", false);
            }

            if (msgFromClient.startsWith("REMOVE_SONG")) {
                String[] parts = msgFromClient.split("\\|", -1);
                if (parts.length == 3) {
                    String eventDate = cl_GetEventDateString(parts[1]);
                    boolean success = cl_RemoveSong(parts[1], parts[2]);
                    if (success) {
                        userActivity.logUserActivity(currentUser, msgFromClient + "|" + eventDate);
                        return new CommandResult("REMOVE_SONG_SUCCESS", true);
                    }
                }
                return new CommandResult("REMOVE_SONG_FAIL", false);
            }

            if (msgFromClient.startsWith("LIST_SONGS")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length == 2) {
                    ArrayList<Song> songs = cl_ListSong(parts[1]);
                    StringBuilder sb = new StringBuilder("Songs List:");
                    for (Song s : songs) {
                        sb.append(s).append("|");
                    }
                    return new CommandResult(sb.toString(), false);
                }
            }

            if (msgFromClient.startsWith("ADD_MEMBER")) {
                String[] parts = msgFromClient.split("\\|", -1);
                if (parts.length >= 3) {
                    String eventTitle = parts[1];
                    String memberName = parts[2];
                    String role = parts.length >= 4 ? parts[3] : "";

                    Event event = calendar.searchByTitle(eventTitle);
                    if (event != null && !memberName.isBlank()) {
                        Member member = usersInfo.getOrCreateMember(memberName, role);
                        event.addtoTeam(member);
                        member.addEvent(event);
                        calendar.save();
                        usersInfo.save();
                        userActivity.logUserActivity(currentUser, msgFromClient);
                        return new CommandResult("ADD_MEMBER_SUCCESS", true);
                    }
                }
                return new CommandResult("ADD_MEMBER_FAIL", false);
            }

            if (msgFromClient.startsWith("REMOVE_MEMBER")) {
                String[] parts = msgFromClient.split("\\|", -1);
                if (parts.length == 3) {
                    Event event = calendar.searchByTitle(parts[1]);
                    Member member = usersInfo.getMemberByName(parts[2]);

                    if (event != null && member != null) {
                        event.removefromTeam(member);
                        member.removeEvent(event);
                        calendar.save();
                        usersInfo.save();
                        userActivity.logUserActivity(currentUser, msgFromClient);
                        return new CommandResult("REMOVE_MEMBER_SUCCESS", true);
                    }
                }
                return new CommandResult("REMOVE_MEMBER_FAIL", false);
            }

            if (msgFromClient.startsWith("LIST_MEMBERS")) {
                String[] parts = msgFromClient.split("\\|");
                if (parts.length == 2) {
                    Event event = calendar.searchByTitle(parts[1]);
                    if (event != null) {
                        StringBuilder sb = new StringBuilder("Members List:");
                        for (Member m : event.getTeam()) {
                            sb.append(formatMemberRole(m)).append("|");
                        }
                        return new CommandResult(sb.toString(), false);
                    }
                }
            }

            System.out.println("invalid command");
            return new CommandResult("INVALID_COMMAND", false);
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket client;
        private BufferedWriter bufferedWriter;
        private String currentUser;
        private boolean subscribedToChanges;

        ClientHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            System.out.println("Client connected:" + client);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
                bufferedWriter = writer;

                String msgFromClient;
                while ((msgFromClient = bufferedReader.readLine()) != null) {
                    msgFromClient = msgFromClient.trim();
                    System.out.println(client + ":" + msgFromClient);

                    if ("QUIT".equals(msgFromClient)) {
                        break;
                    }

                    CommandResult result = handleCommand(msgFromClient, this);
                    System.out.println("[SERVER] Reply to " + client + ": " + result.response);
                    sendLine(result.response);
                    if (result.dataChanged) {
                        broadcastDataChanged(msgFromClient);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + client);
            } finally {
                clients.remove(this);
                System.out.println("[SERVER] Removed client " + client + ". Online clients: " + clients.size());
                try {
                    client.close();
                } catch (IOException ignored) {
                }
                synchronized (DATA_LOCK) {
                    calendar.save();
                    usersInfo.save();
                }
                System.out.println("Server saved all data.");
            }
        }

        synchronized void sendLine(String message) {
            if (bufferedWriter == null) {
                return;
            }
            try {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                clients.remove(this);
            }
        }

        String getCurrentUser() {
            return currentUser;
        }

        void setCurrentUser(String currentUser) {
            this.currentUser = currentUser;
        }

        boolean isSubscribedToChanges() {
            return subscribedToChanges;
        }

        void subscribeToChanges() {
            subscribedToChanges = true;
        }
    }

    private static class CommandResult {
        private final String response;
        private final boolean dataChanged;

        CommandResult(String response, boolean dataChanged) {
            this.response = response;
            this.dataChanged = dataChanged;
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

    public static boolean cl_removeEvent(String title) {
        Event event = calendar.searchByTitle(title);
        if (event == null) {
            return false;
        }
        calendar.removeEvent(event);
        return true;
    }

    public static boolean cl_EditTopic(String eventTitle, String topic) {
        Event event = calendar.searchByTitle(eventTitle);
        if (event == null) {
            return false;
        }
        event.setTopic(topic);
        calendar.save();
        return true;
    }

    public static ArrayList<Event> cl_listEvents(String dateString) {
        LocalDate date = LocalDate.parse(dateString);
        return calendar.getEvents(date);
    }

    public static boolean cl_AddSong(String eventTitle, String name, String author) {
        Event event = calendar.searchByTitle(eventTitle);
        if (event == null || name == null || name.isBlank()) {
            return false;
        }
        event.addSong(name, author);
        calendar.save();
        return true;
    }

    public static boolean cl_RemoveSong(String eventTitle, String name) {
        Event event = calendar.searchByTitle(eventTitle);
        if (event == null || name == null || name.isBlank()) {
            return false;
        }
        boolean removed = event.removeSong(name);
        if (removed) {
            calendar.save();
        }
        return removed;
    }

    public static String cl_GetEventDateString(String eventTitle) {
        Event event = calendar.searchByTitle(eventTitle);
        if (event == null) {
            return "";
        }
        return event.getDate().toString();
    }

    public static ArrayList<Song> cl_ListSong(String eventTitle) {
        Event event = calendar.searchByTitle(eventTitle);
        ArrayList<Song> songs = event.getSongs();
        return songs;
    }

    private static String buildDayDetailResponse(ArrayList<Event> events) {
        StringBuilder sb = new StringBuilder("DAY_DETAIL");
        events.sort(Comparator.comparing(Event::getTime));
        for (Event event : events) {
            String songs = event.getSongs().stream()
                    .map(Song::toString)
                    .collect(Collectors.joining("; "));
            String team = event.getTeam().stream()
                    .map(Server::formatMemberRole)
                    .collect(Collectors.joining("; "));

            sb.append("|")
                    .append(encode(event.getDate().toString())).append(",")
                    .append(encode(event.getTime().toString())).append(",")
                    .append(encode(event.getTitle())).append(",")
                    .append(encode(event.getTopic())).append(",")
                    .append(encode(songs)).append(",")
                    .append(encode(team));
        }
        return sb.toString();
    }

    private static String buildMonthCountsResponse(YearMonth month) {
        StringBuilder sb = new StringBuilder("MONTH_COUNTS");
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            int count = calendar.getEvents(date).size();
            if (count > 0) {
                sb.append("|")
                        .append(encode(date.toString()))
                        .append(",")
                        .append(count);
            }
        }
        return sb.toString();
    }

    private static String formatMemberRole(Member member) {
        String role = member.getRole();
        if (role == null || role.isBlank()) {
            role = "No position";
        }
        return member.getUserName() + " - " + role;
    }

    private static String buildUserActivityResponse() {
        StringBuilder sb = new StringBuilder("USER_ACTIVITY");
        for (String activity : userActivity.getActivities()) {
            sb.append("|").append(encode(activity));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
