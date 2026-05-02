package storage;

import model.Event;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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
                                        e.getTitle()
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
                // 2026-04-27,18:08,Practice
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    LocalDate date = LocalDate.parse(parts[0]);
                    LocalTime time = LocalTime.parse(parts[1]);
                    String title = parts[2];

                    Event e = new Event(date, time, title);

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
}

