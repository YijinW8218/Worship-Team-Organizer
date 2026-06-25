package storage;

import model.Event;
import model.Member;
import model.Song;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.Base64;

public class Storage {

    private static final String fname = "events.csv";


    public static void save(Map<LocalDate, ArrayList<Event>> eventsInDate) {
        try {
            FileWriter filewriter = new FileWriter(fname);
            BufferedWriter writer = new BufferedWriter(filewriter);


            for (LocalDate date : eventsInDate.keySet()) {
                ArrayList<Event> events = eventsInDate.get(date);
                if (events != null) {
                    for (Event e : events) {
                        writer.write(
                                date.toString() + "," +
                                        e.getTime().toString() + "," +
                                        e.getTitle() + "," +
                                        encode(e.getTopic()) + "," +
                                        encodeTeam(e) + "," +
                                        encodeSongs(e)
                        );
                        writer.newLine();
                    }
                }
            }
            writer.flush();
            writer.close();

            System.out.println("[storage.Storage] Saved events to " + fname);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static Map<LocalDate, ArrayList<Event>> load() {
        Map<LocalDate, ArrayList<Event>> map = new HashMap<>();

        File file = new File(fname);
        if (!file.exists()) {
            System.out.println("[storage.Storage] No existing file, starting fresh.");
            return map;
        }

        try {
            FileReader fileReader = new FileReader(fname);
            BufferedReader reader = new BufferedReader(fileReader);

            String line;
            while ((line = reader.readLine()) != null) {
                // 2026-04-27,18:08,Practice,base64-topic
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    LocalDate date = LocalDate.parse(parts[0]);
                    LocalTime time = LocalTime.parse(parts[1]);
                    String title = parts[2];

                    Event e = new Event(date, time, title);
                    if (parts.length >= 4 && !parts[3].isBlank()) {
                        e.setTopic(decode(parts[3]));
                    }
                    if (parts.length >= 5 && !parts[4].isBlank()) {
                        loadTeam(e, decode(parts[4]));
                    }
                    if (parts.length >= 6 && !parts[5].isBlank()) {
                        loadSongs(e, decode(parts[5]));
                    }

                    map.computeIfAbsent(date, k -> new ArrayList<>()).add(e);
                }
            }

            System.out.println("[storage.Storage] Loaded events from " + fname);

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String encodeTeam(Event event) {
        StringBuilder sb = new StringBuilder();
        for (Member member : event.getTeam()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(member.getUserName()).append("\t").append(member.getRole());
        }
        return encode(sb.toString());
    }

    private static void loadTeam(Event event, String teamData) {
        if (teamData == null || teamData.isBlank()) {
            return;
        }

        for (String memberData : teamData.split("\n")) {
            String[] fields = memberData.split("\t", 2);
            if (fields.length >= 1 && !fields[0].isBlank()) {
                String role = fields.length == 2 ? fields[1] : "";
                event.addtoTeam(new Member(-1, fields[0], "", role));
            }
        }
    }

    private static String encodeSongs(Event event) {
        StringBuilder sb = new StringBuilder();
        for (Song song : event.getSongs()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(song.getName()).append("\t").append(song.getAuthor());
        }
        return encode(sb.toString());
    }

    private static void loadSongs(Event event, String songData) {
        if (songData == null || songData.isBlank()) {
            return;
        }

        for (String row : songData.split("\n")) {
            String[] fields = row.split("\t", 2);
            if (fields.length >= 1 && !fields[0].isBlank()) {
                String author = fields.length == 2 ? fields[1] : "";
                event.addSong(fields[0], author);
            }
        }
    }
}
